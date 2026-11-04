package com.bino.binosvanillaplus.client.journal;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.network.PhotoUploadPayload;
import com.bino.binosvanillaplus.network.TripodShotPayload;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Screenshot;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Tira a foto: no proximo quadro o jogo e' desenhado SEM a interface e sem a mao; logo depois o quadro e' copiado,
// recortado no centro e reduzido para 64x64 com poucas cores (pixelado), e vai para o servidor (diario).
// Foto do tripe: no quadro da foto a camera do jogo vai para o tripe e volta em seguida.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class PhotoCapture {
    private PhotoCapture() {}

    public static final int SIZE = 64;
    private static int stage = 0;                // 0 parado, 1 pedir quadro limpo, 2 copiar depois do quadro
    private static boolean fromTripod, prevHide;
    private static BlockPos tripodPos = BlockPos.ZERO;
    private static float tripodYaw;
    private static Entity prevCamera;
    private static long flashAt = -1;

    public static void shoot() {
        if (stage == 0) { fromTripod = false; stage = 1; }
    }

    public static void tripod(TripodShotPayload p) {
        if (stage == 0) { fromTripod = true; tripodPos = p.pos(); tripodYaw = p.yaw(); stage = 1; }
    }

    @SubscribeEvent
    public static void onFramePre(RenderFrameEvent.Pre event) {
        if (stage != 1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { stage = 0; return; }
        prevHide = mc.options.hideGui;
        mc.options.hideGui = true;
        if (fromTripod) {
            ArmorStand eye = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            double y = tripodPos.getY() + 1.05 - eye.getEyeHeight();
            eye.moveTo(tripodPos.getX() + 0.5, y, tripodPos.getZ() + 0.5, tripodYaw, 0f);
            eye.setOldPosAndRot();
            eye.setYHeadRot(tripodYaw);
            eye.yHeadRotO = tripodYaw;
            prevCamera = mc.getCameraEntity();
            mc.setCameraEntity(eye);
        }
        stage = 2;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (stage == 2) event.setCanceled(true);   // a mao/camera nao aparece na foto
    }

    @SubscribeEvent
    public static void onFramePost(RenderFrameEvent.Post event) {
        if (stage != 2) return;
        stage = 0;
        Minecraft mc = Minecraft.getInstance();
        byte[] png = null;
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            png = encode(shot);
        } catch (Exception e) {
            BinosVanillaMod.LOGGER.warn("Foto falhou", e);
        } finally {
            mc.options.hideGui = prevHide;
            if (fromTripod && prevCamera != null) mc.setCameraEntity(prevCamera);
            prevCamera = null;
        }
        if (!fromTripod) flashAt = Util.getMillis();
        if (png != null && png.length <= PhotoUploadPayload.MAX_BYTES)
            PacketDistributor.sendToServer(new PhotoUploadPayload(png, fromTripod, fromTripod ? tripodPos : BlockPos.ZERO));
    }

    // recorte quadrado do centro -> 64x64, cada canal com 5 bits (aspecto de pixel art)
    private static byte[] encode(NativeImage src) throws java.io.IOException {
        int w = src.getWidth(), h = src.getHeight();
        int side = (int) (Math.min(w, h) * 0.9);
        int x0 = (w - side) / 2, y0 = (h - side) / 2;
        try (NativeImage out = new NativeImage(SIZE, SIZE, false)) {
            int step = Math.max(1, side / SIZE / 3);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int sx0 = x0 + x * side / SIZE, sy0 = y0 + y * side / SIZE;
                    int sx1 = x0 + (x + 1) * side / SIZE, sy1 = y0 + (y + 1) * side / SIZE;
                    long r = 0, g = 0, b = 0, n = 0;
                    for (int yy = sy0; yy < sy1; yy += step) {
                        for (int xx = sx0; xx < sx1; xx += step) {
                            int p = src.getPixelRGBA(Math.min(w - 1, xx), Math.min(h - 1, yy));   // ABGR
                            r += p & 255; g += (p >> 8) & 255; b += (p >> 16) & 255; n++;
                        }
                    }
                    if (n == 0) n = 1;
                    int R = q((int) (r / n)), G = q((int) (g / n)), B = q((int) (b / n));
                    out.setPixelRGBA(x, y, 0xFF000000 | (B << 16) | (G << 8) | R);
                }
            }
            return out.asByteArray();
        }
    }

    private static int q(int c) { return Math.min(255, (c >> 3) << 3 | (c >> 5)); }

    // flash: a tela fica branca por um instante
    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "camera_flash"), PhotoCapture::renderFlash);
    }

    private static void renderFlash(GuiGraphics g, DeltaTracker dt) {
        if (flashAt < 0) return;
        float t = (Util.getMillis() - flashAt) / 260f;
        if (t >= 1f) { flashAt = -1; return; }
        int a = (int) (200 * (1f - t) * (1f - t));
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0xFFFFFF);
    }
}
