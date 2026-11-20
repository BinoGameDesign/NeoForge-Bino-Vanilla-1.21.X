package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Camera SOBRE O OMBRO (terceira opcao do F5, a ideia do mod Shoulder Surfing; codigo proprio):
//  F5: primeira pessoa -> ombro -> terceira de costas -> terceira de frente -> primeira pessoa.
// No ombro a camera fica perto, atras e a direita do jogador, e nao atravessa paredes (encosta antes).
// A mira (que o jogo so desenha em primeira pessoa) aparece onde o jogador esta realmente mirando: o ponto que os
// olhos dele acertam, projetado na tela (a camera fica deslocada, entao o meio da tela nao e' o alvo).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ShoulderCam {
    private ShoulderCam() {}

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final double DISTANCE = 2.6;   // atras do jogador (blocos)
    public static final double SIDE = 0.75;      // para a direita (negativo = ombro esquerdo)
    public static final double UP = 0.15;        // acima dos olhos

    private static boolean shoulder;
    private static float blend;   // 0 = camera normal, 1 = no ombro (passa suave)
    private static long lastMs;
    private static double fov = 70;

    public static boolean active() {
        return shoulder && Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK;
    }

    // o F5 passa pelo ombro (pega o clique antes do jogo)
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (mc.options.keyTogglePerspective.consumeClick()) {
            CameraType before = mc.options.getCameraType();
            CameraType next;
            if (before == CameraType.FIRST_PERSON) { next = CameraType.THIRD_PERSON_BACK; shoulder = true; }
            else if (before == CameraType.THIRD_PERSON_BACK && shoulder) { next = CameraType.THIRD_PERSON_BACK; shoulder = false; }
            else if (before == CameraType.THIRD_PERSON_BACK) next = CameraType.THIRD_PERSON_FRONT;
            else { next = CameraType.FIRST_PERSON; }
            mc.options.setCameraType(next);
            if (before.isFirstPerson() != next.isFirstPerson())
                mc.gameRenderer.checkEntityPostEffect(next.isFirstPerson() ? mc.getCameraEntity() : null);
            mc.levelRenderer.needsUpdate();
        }
    }

    // chamado no fim do Camera.setup (CameraRollMixin): nova posicao da camera, ou null
    public static Vec3 position(Camera cam, BlockGetter level, Entity e, float pt) {
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 0f : Mth.clamp((now - lastMs) / 1000f, 0f, 0.1f);
        lastMs = now;
        float target = active() ? 1f : 0f;
        blend += (target - blend) * Math.min(1f, dt * 9f);
        if (target == 0f && blend < 0.01f) { blend = 0f; return null; }
        if (Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) { blend = 0f; return null; }

        Vec3 eye = e.getEyePosition(pt);
        Vector3f f = cam.getLookVector(), l = cam.getLeftVector();
        Vec3 fwd = new Vec3(f.x, f.y, f.z), right = new Vec3(-l.x, -l.y, -l.z);
        // primeiro de lado (ate o ombro), depois para tras: cada trecho para antes de uma parede
        Vec3 side = right.scale(SIDE).add(0, UP, 0);
        double sideLen = side.length();
        Vec3 sideDir = side.scale(1 / sideLen);
        Vec3 shoulderPt = eye.add(sideDir.scale(free(level, e, eye, sideDir, sideLen)));
        Vec3 back = fwd.scale(-1);
        Vec3 pos = shoulderPt.add(back.scale(free(level, e, shoulderPt, back, DISTANCE)));
        Vec3 vanilla = cam.getPosition();
        return vanilla.lerp(pos, smooth(blend));
    }

    // quanto da para andar de "from" na direcao "dir" ate encostar num bloco (8 raios em volta, como o jogo faz)
    private static double free(BlockGetter level, Entity e, Vec3 from, Vec3 dir, double len) {
        double best = len;
        for (int i = 0; i < 8; i++) {
            double ox = ((i & 1) * 2 - 1) * 0.1, oy = ((i >> 1 & 1) * 2 - 1) * 0.1, oz = ((i >> 2 & 1) * 2 - 1) * 0.1;
            Vec3 a = from.add(ox, oy, oz), b = a.add(dir.scale(len));
            HitResult hit = level.clip(new ClipContext(a, b, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e));
            if (hit.getType() != HitResult.Type.MISS) best = Math.min(best, hit.getLocation().distanceTo(a));
        }
        return Math.max(0, best - 0.12);
    }

    private static float smooth(float a) { a = Mth.clamp(a, 0f, 1f); return a * a * (3 - 2 * a); }

    // ---------------------------------------------------------------- mira
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (e.usedConfiguredFov()) fov = e.getFOV();
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "shoulder_crosshair"), ShoulderCam::renderCrosshair);
    }

    private static final ResourceLocation CROSSHAIR = ResourceLocation.withDefaultNamespace("hud/crosshair");

    private static void renderCrosshair(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || blend < 0.5f || mc.options.hideGui || mc.player == null || mc.screen != null) return;
        Entity ent = mc.getCameraEntity();
        if (ent == null) return;
        float pt = dt.getGameTimeDeltaPartialTick(true);
        Vec3 eye = ent.getEyePosition(pt), look = ent.getViewVector(pt);
        // o ponto mirado: o que o jogo acertaria, ou a primeira parede ate 64 blocos
        Vec3 aim;
        HitResult hr = mc.hitResult;
        if (hr != null && hr.getType() != HitResult.Type.MISS) aim = hr.getLocation();
        else {
            HitResult far = ent.level().clip(new ClipContext(eye, eye.add(look.scale(64)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, ent));
            aim = far.getType() != HitResult.Type.MISS ? far.getLocation() : eye.add(look.scale(64));
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 c = cam.getPosition();
        Vector3f v = new Vector3f((float) (aim.x - c.x), (float) (aim.y - c.y), (float) (aim.z - c.z));
        new Quaternionf(cam.rotation()).conjugate().transform(v);   // para o espaco da camera (frente = -Z)
        if (v.z > -0.05f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        double focal = (h / 2.0) / Math.tan(Math.toRadians(fov) / 2);
        int x = (int) Math.round(w / 2.0 + v.x / -v.z * focal), y = (int) Math.round(h / 2.0 - v.y / -v.z * focal);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);   // como a mira do jogo (inverte a cor)
        g.blitSprite(CROSSHAIR, x - 7, y - 7, 15, 15);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
