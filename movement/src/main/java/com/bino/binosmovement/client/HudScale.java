package com.bino.binosmovement.client;

import com.bino.binosmovement.BinosMovement;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import java.util.Set;

// Tamanho do visor de baixo (hotbar, vida, armadura, fome, sede, ar, experiencia, temperatura, estamina) independente
// da "Escala da interface" do jogo: cresce/encolhe a partir do meio da base da tela. O valor vem da opcao do Vanilla+
// (ClientConfig.HUD_SCALE); os desenhos que nao sao camadas (estamina, numero de nivel) chamam push/pop.
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class HudScale {
    private HudScale() {}

    public static float scale = 1f;

    private static final Set<String> LAYERS = Set.of(
            "minecraft:hotbar", "minecraft:jump_meter", "minecraft:experience_bar", "minecraft:player_health",
            "minecraft:armor_level", "minecraft:food_level", "minecraft:vehicle_health", "minecraft:air_level",
            "minecraft:experience_level", "minecraft:selected_item_name", "minecraft:overlay_message",
            "binosvanillaplus:thirst", "binosvanillaplus:temperature");

    public static boolean active() { return Math.abs(scale - 1f) > 0.001f; }

    public static void push(GuiGraphics g) {
        g.pose().pushPose();
        if (!active()) return;
        float cx = g.guiWidth() / 2f, by = g.guiHeight();
        g.pose().translate(cx, by, 0);
        g.pose().scale(scale, scale, 1f);
        g.pose().translate(-cx, -by, 0);
    }

    public static void pop(GuiGraphics g) { g.pose().popPose(); }

    private static boolean pushed;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPre(RenderGuiLayerEvent.Pre e) {
        if (!LAYERS.contains(e.getName().toString())) return;
        push(e.getGuiGraphics());
        pushed = true;
    }

    // outro mod cancelou a camada: o Post nao vem, entao desfaz aqui
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPreCanceled(RenderGuiLayerEvent.Pre e) {
        if (pushed && e.isCanceled() && LAYERS.contains(e.getName().toString())) { pop(e.getGuiGraphics()); pushed = false; }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPost(RenderGuiLayerEvent.Post e) {
        if (pushed && LAYERS.contains(e.getName().toString())) { pop(e.getGuiGraphics()); pushed = false; }
    }
}
