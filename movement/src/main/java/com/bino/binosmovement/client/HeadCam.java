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

    // deslocamento da camera (blocos, no mundo) pela animacao; null = sem animacao de corpo agora
    public static Vec3 offset(Player p, float pt) {
        has = false;
        if (!enabled() || p != Minecraft.getInstance().getCameraEntity()) return null;
        MovementHooks.BodyFrame f = MovementHooks.bodyFrame(p, pt);
        if (f == null) return null;
        float w = f.weight() * MovementConfig.intensity();
        BbAnims.Sample s = BbAnims.weighted(f.sample(), Math.min(1f, w));
        if (MovementConfig.shortSpins()) s = limited(s);
        Matrix4f body = BbAnims.bodyMatrix(s, f.headShare());
        float px = BbAnims.px();
        float eyePx = p.getEyeHeight() / px;
        Vector3f eye = new Vector3f(0, eyePx, 0);
        Vector3f moved = body.transformPosition(new Vector3f(eye));
        Vector3f d = moved.sub(eye).mul(px);
        // espaco do Blockbench -> mundo: gira para onde o jogador olha (e espelha para canhoto). E' o olhar, nao o
        // tronco: a cambalhota sai para a frente da tela (os bracos da primeira pessoa usam o mesmo)
        float yaw = p.getViewYRot(pt);
        Quaternionf frame = new Quaternionf().rotationY((180f - yaw) * Mth.DEG_TO_RAD);
        Quaternionf q = body.getUnnormalizedRotation(new Quaternionf()).normalize();
        if (!f.rightHanded()) { d.x = -d.x; q.set(q.x, -q.y, -q.z, q.w); }
        frame.transform(d);
        // giro no mundo = frame * q * frame^-1
        ROT.set(frame).mul(q).mul(new Quaternionf(frame).conjugate());
        has = true;
        return new Vec3(d.x, d.y, d.z);
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
