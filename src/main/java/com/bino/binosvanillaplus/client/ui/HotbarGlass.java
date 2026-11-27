package com.bino.binosvanillaplus.client.ui;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Hotbar com o mesmo vidro do inventario (27/11): painel de vidro escuro desfocado com sombra, pocos de vidro afundados
// e a moldura branca "select" no item escolhido. Os desenhos do jogo (hud/hotbar*) ficaram transparentes
// (tools/ui/gen_hotbar.py): o jogo so desenha os itens por cima. Sem os shaders do vidro: retangulos arredondados
// nas mesmas cores do inventario.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class HotbarGlass {
    private HotbarGlass() {}

    private static final ResourceLocation SELECT = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/inventory/select.png");
    private static final int PANEL = 0xA8222426, SLOT = 0xD6212121;   // mesmas cores do inventario
    private static final Glass.Style HUD_PANEL = Glass.PANEL.withTint(0.55f);
    private static final float CELL = 18f, RADIUS = 5f;

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (e.isCanceled() || !e.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        if (!(mc.getCameraEntity() instanceof Player p)) return;
        GuiGraphics g = e.getGuiGraphics();
        int cx = g.guiWidth() / 2, h = g.guiHeight();
        Glass.capture();   // o mundo atras do visor, desfocado (uma vez por quadro)

        // painel principal (no mesmo lugar do desenho do jogo: 182x22) e o da mao secundaria
        float x = cx - 91, y = h - 22;
        glass(g, x, y, 182, 22, RADIUS, true, null);
        boolean off = !p.getOffhandItem().isEmpty();
        boolean offLeft = p.getMainArm().getOpposite() == HumanoidArm.LEFT;
        float ox = offLeft ? cx - 91 - 29 : cx + 91 + 7;
        if (off) glass(g, ox, y, 22, 22, RADIUS, true, null);

        // pocos (o item do jogo fica em x+3, y+3: poco de 18 com 1 px em volta)
        for (int i = 0; i < 9; i++) glass(g, x + 2 + 20 * i, y + 2, CELL, CELL, CELL * 0.17f, false, Glass.SLOT);
        if (off) glass(g, ox + 2, y + 2, CELL, CELL, CELL * 0.17f, false, Glass.SLOT);

        // moldura branca do item escolhido (a mesma "select" do inventario)
        int sel = p.getInventory().selected;
        float sx = x + 2 + 20 * sel - CELL * 0.035f, sy = y + 2 - CELL * 0.035f, ss = CELL * 1.07f;
        g.flush();
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        g.pose().scale(ss / 16f, ss / 16f, 1f);
        g.blit(SELECT, 0, 0, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
    }

    // um pedaco de vidro em coordenadas da GUI. O vidro ignora a pose (usa a posicao final na tela), entao a escala do
    // visor (HudScale) e' aplicada aqui antes.
    private static void glass(GuiGraphics g, float x, float y, float w, float h, float r, boolean panel, Glass.Style st) {
        Matrix4f m = g.pose().last().pose();
        Vector3f a = m.transformPosition(new Vector3f(x, y, 0)), b = m.transformPosition(new Vector3f(x + w, y + h, 0));
        float k = (b.x - a.x) / w;
        g.pose().pushPose();
        g.pose().last().pose().identity();
        if (panel) {
            Glass.shadow(g, a.x, a.y, b.x - a.x, b.y - a.y, r * k, 2.5f * k, 5f * k, 0.35f);
            Glass.panel(g, a.x, a.y, b.x - a.x, b.y - a.y, r * k, HUD_PANEL, PANEL);
        } else {
            Glass.panel(g, a.x, a.y, b.x - a.x, b.y - a.y, r * k, st, SLOT);
        }
        g.pose().popPose();
    }
}
