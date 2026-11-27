package com.bino.binosvanillaplus.client.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.thirst.Temperature;
import com.bino.binosvanillaplus.thirst.TemperatureSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Temperatura no cliente: o termometro do Bino (5 cores pela faixa) e a temperatura do ambiente em °C, no canto
// esquerdo, ao lado dos coracoes (equilibra com a estamina do lado direito). Frio forte: tremedeira leve na camera;
// calor forte: borda quente na tela. Forro na descricao da armadura.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTemperature {
    private ClientTemperature() {}

    private static int ambient = 20, exposure = 0;
    private static boolean has;

    public static void receive(TemperatureSyncPayload m) { ambient = m.ambient(); exposure = m.exposure(); has = true; }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { has = false; ambient = 20; exposure = 0; }

    private static ResourceLocation sprite(String n) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "hud/thermometer_" + n); }
    private static final ResourceLocation[] THERMO = {sprite("freezing"), sprite("cold"), sprite("mild"), sprite("hot"), sprite("scorching")};

    /** faixa do termometro: 0 congelante, 1 frio, 2 agradavel, 3 quente, 4 escaldante */
    public static int band(int c) { return c <= 0 ? 0 : c <= 14 ? 1 : c <= 27 ? 2 : c <= 37 ? 3 : 4; }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "temperature"), ClientTemperature::render);
        e.registerBelow(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "heat_vignette"), ClientTemperature::vignette);
    }

    private static final float SCALE = 2f;   // termometro e numero no dobro (do tamanho do desenho do Bino)

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!has || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        if (!(mc.getCameraEntity() instanceof Player)) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float right = w / 2f - 91 - 26;                    // deixa espaco para as barras de experiencia (numero verde)
        float tx = right - 10 * SCALE, ty = h - 21 * SCALE - 1;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0);
        g.pose().scale(SCALE, SCALE, 1f);
        g.blitSprite(THERMO[band(ambient)], 0, 0, 10, 21);
        g.pose().popPose();
        var font = mc.font;
        String num = String.valueOf(ambient), unit = "°C";
        float k = SCALE;
        textRight(g, num, tx - 2, ty + 2, k);
        textRight(g, unit, tx - 2, ty + 2 + 9 * k + 1, k);
    }

    private static void textRight(GuiGraphics g, String s, float rightX, float y, float k) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(rightX - font.width(s) * k, y, 0);
        g.pose().scale(k, k, 1f);
        g.drawString(font, s, 1, 0, 0xFF3A3A3A, false);
        g.drawString(font, s, 0, 1, 0xFF3A3A3A, false);
        g.drawString(font, s, 0, 0, 0xFFE8E8E8, false);
        g.pose().popPose();
    }

    // calor forte: bordas alaranjadas na tela (etapa 2 fraca, etapa 3 mais forte)
    private static void vignette(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!has || exposure < Temperature.STAGE_2 || mc.options.hideGui) return;
        float k = exposure >= Temperature.STAGE_3 ? 1f : 0.55f;
        float pulse = 0.85f + 0.15f * Mth.sin((mc.player != null ? mc.player.tickCount : 0) * 0.15f);
        int a = (int) (90 * k * pulse);
        int edge = (a << 24) | 0xE05A10, none = 0x00E05A10;
        int w = g.guiWidth(), h = g.guiHeight(), d = h / 5;
        g.fillGradient(0, 0, w, d, edge, none);
        g.fillGradient(0, h - d, w, h, none, edge);
    }

    // frio forte: a camera treme de leve (etapa 2) e um pouco mais (etapa 3)
    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!has || -exposure < Temperature.STAGE_2) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        float k = -exposure >= Temperature.STAGE_3 ? 0.6f : 0.3f;
        float t = (mc.player.tickCount + (float) e.getPartialTick()) * 1.7f;
        e.setRoll(e.getRoll() + Mth.sin(t) * k);
        e.setYaw(e.getYaw() + Mth.sin(t * 1.3f + 1f) * k * 0.5f);
    }

    // descricao: forro da armadura
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        int l = e.getItemStack().getOrDefault(ModDataComponents.LINING.get(), 0);
        if (l == Temperature.LINING_WOOL) e.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.lining.wool").withStyle(ChatFormatting.GOLD));
        else if (l == Temperature.LINING_LIGHT) e.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.lining.light").withStyle(ChatFormatting.AQUA));
    }
}
