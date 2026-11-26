package com.bino.binosmovement.client;

import com.bino.binosmovement.BinosMovement;
import com.bino.binosmovement.MovementConfig;
import com.bino.binosmovement.client.anim.BbAnims;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Camera de primeira pessoa presa a cabeca da animacao do corpo (Blockbench): o corpo inteiro (root + tumble) e um
// pouco da cabeca levam a camera junto. A cambalhota do Juizo gira a tela, o rodopio da Tormenta vira a tela, a
// estocada leva a camera para a frente, o tombo rola a tela.
//  - a MIRA nao gira: o jogador continua olhando/acertando para onde mirou (so a imagem gira)
//  - opcoes: MovementConfig (liga/desliga, intensidade, "giro curto" para quem enjoa)
// Contas: a animacao vira uma matriz no espaco do Blockbench (BbAnims.bodyMatrix); o olho do jogador passa por ela
// (deslocamento) e a rotacao dela gira a camera (no evento dos angulos).
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class HeadCam {
    private HeadCam() {}

    private static boolean has;
    private static final Quaternionf ROT = new Quaternionf();   // giro do corpo no mundo (para a camera)

    public static void clear() { has = false; }

    // a camera esta girada pela animacao agora? (a mira passa a seguir o ponto mirado, nao o centro da tela)
    public static boolean turning() { return has && Math.abs(ROT.w) < 0.9995f; }

    public static boolean enabled() {
        Minecraft mc = Minecraft.getInstance();
        return MovementConfig.headCamera() && MovementConfig.intensity() > 0.001f && mc.options.getCameraType().isFirstPerson();
    }

    // ===== suavidade (25/11: "a camera ta muito forte e puxando muito")
    private static final float MOVE_SHARE = 0.45f;      // quanto do deslocamento do corpo a camera acompanha
    private static final float MAX_FORWARD = 0.30f;     // no maximo isso para a frente/os lados (blocos): nao entra no mob
    private static final float MAX_DOWN = 0.35f, MAX_UP = 0.25f;
    private static final float TILT_SHARE = 0.45f;      // inclinacoes pequenas: so isso da inclinacao do corpo
    private static final float SPIN_FULL_FROM = 150f;   // giros grandes (cambalhota, rodopio) vao inteiros a partir daqui
    private static final float SMOOTH_S = 0.07f;        // a camera alcanca a animacao nesse tempo (segundos)
    private static final float BREATH_UP = 0.008f;      // respiracao: blocos para cima com o peito cheio
    private static final float BREATH_PITCH = -0.25f;   // respiracao: graus (negativo = olha um tiquinho para cima)

    private static float breathPitch;
    private static final Vector3f OFF = new Vector3f();
    private static final Quaternionf SMOOTH_ROT = new Quaternionf();
    private static long lastMs;

    // deslocamento da camera (blocos, no mundo) pela animacao; null = sem animacao de corpo agora (a camera volta suave)
    public static Vec3 offset(Player p, float pt) {
        has = false;
        if (!enabled() || p != Minecraft.getInstance().getCameraEntity()) { OFF.zero(); SMOOTH_ROT.identity(); return null; }
        Vector3f target = new Vector3f();
        Quaternionf targetRot = new Quaternionf();
        MovementHooks.BodyFrame f = MovementHooks.bodyFrame(p, pt);
        if (f != null) {
            float w = f.weight() * MovementConfig.intensity();
            BbAnims.Sample s = BbAnims.weighted(f.sample(), Math.min(1f, w));
            if (MovementConfig.shortSpins()) s = limited(s);
            Matrix4f body = BbAnims.bodyMatrix(s, f.headShare());
            float px = BbAnims.px();
            Vector3f eye = new Vector3f(0, p.getEyeHeight() / px, 0);
            Vector3f d = body.transformPosition(new Vector3f(eye)).sub(eye).mul(px * MOVE_SHARE);
            d.x = Mth.clamp(d.x, -MAX_FORWARD, MAX_FORWARD);
            d.z = Mth.clamp(d.z, -MAX_FORWARD, MAX_FORWARD);
            d.y = Mth.clamp(d.y, -MAX_DOWN, MAX_UP);
            // espaco do Blockbench -> mundo: gira para onde o jogador olha (e espelha para canhoto). E' o olhar, nao o
            // tronco: a cambalhota sai para a frente da tela (os bracos da primeira pessoa usam o mesmo)
            Quaternionf frame = new Quaternionf().rotationY((180f - p.getViewYRot(pt)) * Mth.DEG_TO_RAD);
            Quaternionf q = body.getUnnormalizedRotation(new Quaternionf()).normalize();
            if (!f.rightHanded()) { d.x = -d.x; q.set(q.x, -q.y, -q.z, q.w); }
            q = gentle(q, f.rotShare());
            frame.transform(d);
            target.set(d);
            // giro no mundo = frame * q * frame^-1
            targetRot.set(frame).mul(q).mul(new Quaternionf(frame).conjugate());
        }
        // respiracao: a camera sobe e desce bem de leve e inclina um tiquinho (segue "Movimento da visao")
        if (Minecraft.getInstance().options.bobView().get()) {
            float br = com.bino.binosmovement.client.anim.Locomotion.breath(p) * MovementConfig.intensity();
            target.y += BREATH_UP * br;
            breathPitch = BREATH_PITCH * br;
        } else breathPitch = 0f;
        // suaviza: a camera chega na animacao (e volta dela) sem tranco
        long now = net.minecraft.Util.getMillis();
        float dt = lastMs == 0 ? 1f : Math.min(0.25f, (now - lastMs) / 1000f);
        lastMs = now;
        float k = 1f - (float) Math.exp(-dt / SMOOTH_S);
        OFF.lerp(target, k);
        if (SMOOTH_ROT.dot(targetRot) < 0) targetRot.mul(-1f);   // mesmo lado da esfera: nao da a volta longa
        SMOOTH_ROT.slerp(targetRot, k).normalize();
        boolean moving = OFF.lengthSquared() > 1e-8f || Math.abs(SMOOTH_ROT.w) < 0.99999f;
        if (!moving) return null;
        ROT.set(SMOOTH_ROT);
        has = true;
        return new Vec3(OFF.x, OFF.y, OFF.z);
    }

    // inclinacoes pequenas ficam mais fracas (TILT_SHARE); giros grandes (cambalhota do Juizo, rodopio da Tormenta)
    // crescem ate inteiros, sem pulo no meio
    private static Quaternionf gentle(Quaternionf q, float share) {
        float ang = 2f * (float) Math.acos(Mth.clamp(Math.abs(q.w), 0f, 1f)) * Mth.RAD_TO_DEG;   // 0..180
        if (ang < 0.01f) return q;
        float small = TILT_SHARE * share;
        float t = Mth.clamp((ang - 60f) / (SPIN_FULL_FROM - 60f), 0f, 1f);
        t = t * t * (3 - 2 * t);
        float out = ang * Mth.lerp(t, small, 1f);
        return new Quaternionf().slerp(q, out / ang);
    }

    // "giro curto": cambalhota e rodopio ficam so uma inclinacao
    private static BbAnims.Sample limited(BbAnims.Sample a) {
        float max = MovementConfig.SHORT_SPIN_MAX;
        return new BbAnims.Sample() {
            public float[] rot(String b) {
                float[] v = a.rot(b);
                if (b.equals("root") || b.equals("tumble")) for (int i = 0; i < 3; i++) v[i] = Mth.clamp(Mth.wrapDegrees(v[i]), -max, max);
                return v;
            }
            public float[] pos(String b) { return a.pos(b); }
            public float[] scale(String b) { return a.scale(b); }
        };
    }

    // gira a camera: orientacao final = giro do corpo * orientacao normal (yaw, pitch, roll do jogo)
    // A camera do jogo e' Ry(180 - yaw) * Rx(-pitch) * Rz(-roll); a de volta sai pela ordem YXZ.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!has) return;
        e.setPitch(e.getPitch() + breathPitch);   // respiracao: inclina um tiquinho
        float d = Mth.DEG_TO_RAD;
        Quaternionf base = new Quaternionf().rotationYXZ(Mth.PI - e.getYaw() * d, -e.getPitch() * d, -e.getRoll() * d);
        Quaternionf t = new Quaternionf(ROT).mul(base);
        Vector3f eul = t.getEulerAnglesYXZ(new Vector3f());
        e.setYaw(180f - eul.y * Mth.RAD_TO_DEG);
        e.setPitch(-eul.x * Mth.RAD_TO_DEG);
        e.setRoll(-eul.z * Mth.RAD_TO_DEG);
    }

    // onde fica na tela o ponto para onde o jogador mira (com a camera girada pela animacao); null = centro da tela
    public static int[] aimOnScreen(int guiW, int guiH) {
        if (!turning()) return null;
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        if (mc.player == null) return null;
        Vec3 look = mc.player.getViewVector(1f);
        Vector3f v = new Vector3f((float) look.x, (float) look.y, (float) look.z);
        new Quaternionf(cam.rotation()).conjugate().transform(v);   // espaco da camera (frente = -Z)
        if (v.z > -0.05f) return new int[]{-10000, -10000};          // atras da camera: nao desenha
        double fov = mc.options.fov().get();
        double focal = (guiH / 2.0) / Math.tan(Math.toRadians(fov) / 2);
        return new int[]{(int) Math.round(guiW / 2.0 + v.x / -v.z * focal), (int) Math.round(guiH / 2.0 - v.y / -v.z * focal)};
    }
}
