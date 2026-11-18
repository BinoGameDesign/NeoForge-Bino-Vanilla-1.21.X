package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import com.bino.binosvanillaplus.client.LapidaryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Liga a cena da Mesa de Lapidacao ao jogo: modelos das pecas, desenho da mesa, troca da janela pela cena, camera,
// bracos, campo de visao, HUD escondido e o movimento travado.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class LapidaryClientEvents {

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(LapidaryGeo.DISC);
        event.register(LapidaryGeo.ARM);
        event.register(LapidaryGeo.BUTTON_MODEL);
        event.register(LapidaryGeo.VISOR_MODEL);
        event.register(com.bino.binosvanillaplus.client.trap.RatTrapRenderer.BAIL);   // arco da ratoeira
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LAPIDARY_TABLE.get(), LapidaryTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RAT_TRAP.get(), com.bino.binosvanillaplus.client.trap.RatTrapRenderer::new);
    }

    // a janela antiga ia abrir: no lugar dela entra a cena. Durante a cena, o menu de pausa (janela perdeu o foco)
    // nao abre por cima, senao a cena terminaria.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof LapidaryScreen && LapidaryScene.begin()) event.setNewScreen(new LapidarySceneScreen());
        else if (event.getNewScreen() instanceof PauseScreen && Minecraft.getInstance().screen instanceof LapidarySceneScreen) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.LAPIDARY_TABLE.get()))
            LapidaryScene.clicked(event.getPos());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!LapidaryScene.active()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            LapidaryPick.capture(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            LapidaryHands.render(event.getPoseStack(), buf, event.getCamera());
            buf.endBatch();
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (LapidaryScene.active() && event.usedConfiguredFov()) event.setFOV(LapidaryScene.fov(event.getFOV()));
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (LapidaryScene.active()) event.setCanceled(true);   // a cena desenha os proprios bracos
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (LapidaryScene.active()) event.setCanceled(true);   // sem HUD durante a cena
    }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        if (LapidaryScene.active()) event.setCanceled(true);   // sem contorno preto de bloco
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LapidaryScene.movementInput(event.getInput());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        LapidaryScene.tick();
        LapidaryRemote.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LapidaryScene.abort();
        LapidaryRemote.clear();
        LapidaryDraw.clear();
        LapidaryPick.invalidate();
    }
}
