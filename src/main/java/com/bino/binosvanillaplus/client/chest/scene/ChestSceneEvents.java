package com.bino.binosvanillaplus.client.chest.scene;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.bino.binosvanillaplus.client.lapidary.LapidaryScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
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

// Liga a cena do bau ao jogo (troca da janela, camera via CameraLapidaryMixin, campo de visao, HUD/mao escondidos).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class ChestSceneEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof ContainerScreen cs && !LapidaryScene.active() && ChestScene.begin(cs.getMenu()))
            event.setNewScreen(new ChestSceneScreen(cs.getTitle()));
        else if (event.getNewScreen() instanceof PauseScreen && Minecraft.getInstance().screen instanceof ChestSceneScreen) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && ChestScene.isChest(event.getLevel().getBlockState(event.getPos())))
            ChestScene.clicked(event.getPos());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!ChestScene.active()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            LapidaryPick.capture(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            ChestScene.render(event.getPoseStack(), buf, event.getCamera());
            buf.endBatch();
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (ChestScene.active() && event.usedConfiguredFov()) event.setFOV(ChestScene.fov(event.getFOV()));
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) { if (ChestScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) { if (ChestScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) { if (ChestScene.active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) { ChestScene.movementInput(event.getInput()); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) { ChestScene.tick(); }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { ChestScene.abort(); }
}
