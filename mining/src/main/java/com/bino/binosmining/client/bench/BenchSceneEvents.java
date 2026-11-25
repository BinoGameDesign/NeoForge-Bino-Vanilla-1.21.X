package com.bino.binosmining.client.bench;

import com.bino.binosmining.BinosMining;
import com.bino.binosmining.client.lapidary.LapidaryPick;
import com.bino.binosmining.client.lapidary.LapidaryScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Liga a cena da bancada de trabalho e da bigorna ao jogo. Faz parte do mod MINING (separado da cena do bau):
// so depende das pecas da Mesa de Lapidacao (camera, bracos, raio do mouse) e do kit de UI.
@EventBusSubscriber(modid = BinosMining.MOD_ID, value = Dist.CLIENT)
public class BenchSceneEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if ((event.getNewScreen() instanceof CraftingScreen || event.getNewScreen() instanceof AnvilScreen)
                && event.getNewScreen() instanceof AbstractContainerScreen<?> acs && !LapidaryScene.active()
                && !(Minecraft.getInstance().screen instanceof BenchSceneScreen) && BenchScene.begin(acs.getMenu()))
            event.setNewScreen(new BenchSceneScreen(acs.getTitle()));
        else if (event.getNewScreen() instanceof PauseScreen && Minecraft.getInstance().screen instanceof BenchSceneScreen) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) BenchScene.clicked(event.getPos());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!BenchScene.active()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) LapidaryPick.capture(event);
        else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            BenchScene.render(event.getPoseStack(), buf, event.getCamera());
            buf.endBatch();
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (BenchScene.active() && event.usedConfiguredFov()) event.setFOV(BenchScene.fov(event.getFOV()));
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) { if (BenchScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) { if (BenchScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) { if (BenchScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) { BenchScene.movementInput(event.getInput()); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) { BenchScene.tick(); }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { BenchScene.abort(); }
}
