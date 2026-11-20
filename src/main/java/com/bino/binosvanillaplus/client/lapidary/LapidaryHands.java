package com.bino.binosvanillaplus.client.lapidary;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Os bracos do jogador na cena (com a skin dele). Cada mao e' um ponto no mundo que persegue um alvo com uma mola
// (movimento suave, com um leve exagero); o braco e' esticado do ombro ate a mao. Mao direita: segue o mouse / leva o
// item / pega a gema. Mao esquerda: aperta o botao. Jeito de desenhar o braco inspirado no Cinematic Interactions (MIT).
public final class LapidaryHands {
    private LapidaryHands() {}

    public static final class Spring {
        public Vec3 pos = Vec3.ZERO, vel = Vec3.ZERO;
        public boolean init;

        public void step(Vec3 target, double dt, double omega) {
            if (!init) { pos = target; vel = Vec3.ZERO; init = true; return; }
            // mola criticamente amortecida, em passos pequenos
            int n = Math.max(1, (int) Math.ceil(dt / (1 / 120.0)));
            double h = dt / n;
            for (int i = 0; i < n; i++) {
                Vec3 acc = target.subtract(pos).scale(omega * omega).subtract(vel.scale(2 * omega * 0.92));
                vel = vel.add(acc.scale(h));
                pos = pos.add(vel.scale(h));
            }
        }
    }

    // braco: espessura (1 = braco do jogador). O braco inteiro e' desenhado (cortar no antebraco deixava a ponta
    // aberta aparecendo); os ombros ficam baixos e para fora, entao so o fim do braco entra na tela.
    static final float ARM_THICK = 0.5f;

    // distancia da camera (em blocos) a que a mao fica quando alcanca a barra de itens
    private static final double BAR_REACH = 0.62;

    private static final Spring RIGHT = new Spring(), LEFT = new Spring();
    private static double tapAt = -100, lastFrame;

    static void begin() {
        RIGHT.init = LEFT.init = false;
        lastFrame = LapidaryScene.clock();
    }

    static void tap() { tapAt = LapidaryScene.clock(); }

    // ---------------------------------------------------------------- referencias da camera
    public static Vec3 forward(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    public static Vec3 rightOf(Vec3 fwd) { return new Vec3(-fwd.z, 0, fwd.x); }

    public static Vec3 shoulder(boolean right, Vec3 cam, float yaw) {
        Vec3 f = forward(yaw);
        return cam.add(rightOf(f).scale(right ? 0.42 : -0.42)).add(0, -0.6, 0).add(f.scale(0.02));
    }

    // mao "guardada" (fora da tela, perto do ombro) para entrar/sair da cena
    public static Vec3 hidden(boolean right, Vec3 cam, float yaw) {
        return shoulder(right, cam, yaw).add(forward(yaw).scale(0.1)).add(0, -0.08, 0);
    }

    // ---------------------------------------------------------------- alvos
    private static Vec3 p(double x, double y, double z) { return LapidaryScene.point(x, y, z); }
    private static Vec3 p(Vec3 v) { return LapidaryScene.point(v); }

    static void update(Vec3 cam, float yaw) {
        double now = LapidaryScene.clock();
        double dt = Mth.clamp(now - lastFrame, 0, 0.1);
        lastFrame = now;
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 towardCam = cam.subtract(p(LapidaryGeo.LOOK)).multiply(1, 0, 1).normalize();

        Vec3 tR = p(LapidaryGeo.REST_RIGHT), tL = p(LapidaryGeo.REST_LEFT);
        double omegaR = 15, omegaL = 15;

        if (LapidaryScene.cutting()) {
            double t = now - LapidaryScene.cutAt;
            boolean all = LapidaryScene.cutAll;
            double e = LapidaryCut.extra(all);
            // esquerda: aperta o botao (chega, afunda, solta)
            if (t < 0.55) {
                double press = LapidaryCut.hump(LapidaryCut.window(t, 0.12, 0.42));
                tL = p(LapidaryGeo.BUTTON.x - 0.9 + 0.9 * press, LapidaryGeo.BUTTON.y + 0.2, LapidaryGeo.BUTTON.z);
                omegaL = 22;
            }
            // direita: vai ate o disco, pega a gema e leva ate o canto das gemas
            double a = LapidaryCut.reachAt(all), pick = LapidaryCut.pickAt(all), place = LapidaryCut.placeAt(all);
            Vec3 disc = p(LapidaryGeo.ORE).add(0, 0.05, 0).add(towardCam.scale(0.03));
            Vec3 gems = p(LapidaryGeo.GEMS).add(0, 0.05, 0).add(towardCam.scale(0.03));
            if (t >= a - 0.25 && t < pick) {
                tR = disc.add(0, 0.06 * (1 - LapidaryCut.window(t, a, pick)), 0);
                omegaR = 17;
            } else if (t >= pick && t < place) {
                double u = LapidaryCut.inOutCubic((t - pick) / (place - pick));
                tR = disc.add(gems.subtract(disc).scale(u)).add(0, 0.11 * LapidaryCut.hump(u), 0);
                omegaR = 24;
            } else if (t >= place && t < place + 0.12) {
                tR = gems;
            }
        } else if (LapidaryScene.phase == LapidaryScene.Phase.ACTIVE && !LapidaryScene.placing.isEmpty()) {
            // levando um item da barra ate a mesa: a mao acompanha o item
            LapidaryScene.Placing pl = LapidaryScene.placing.get(LapidaryScene.placing.size() - 1);
            tR = placingAt(pl, cam, yaw, now).add(0, 0.03, 0).add(towardCam.scale(0.04));
            omegaR = 26;
        } else if (LapidaryScene.phase == LapidaryScene.Phase.ACTIVE) {
            switch (LapidaryScene.hover) {
                case BUTTON -> tL = p(LapidaryGeo.BUTTON.x - 1.0, LapidaryGeo.BUTTON.y + 0.2, LapidaryGeo.BUTTON.z);
                case PILE -> tR = p(LapidaryGeo.GEMS).add(0, 0.07, 0).add(towardCam.scale(0.03));
                case FLINT, ORE -> tR = LapidaryScene.targetPoint(LapidaryScene.hover, LapidaryScene.localView())
                        .add(0, LapidaryScene.hover == LapidaryScene.Target.FLINT ? -0.02 : 0.07, 0).add(towardCam.scale(0.05));
                default -> {
                    // sem alvo: a mao fica descansando e so "olha" um pouco para onde o mouse esta (antes ela
                    // seguia o mouse por cima da mesa e ate subia ate a barra, cobrindo a cena inteira)
                    Vec3 top = LapidaryScene.overBar ? null : LapidaryScene.onTop();
                    if (top != null) tR = tR.add(top.add(0, 0.07, 0).subtract(tR).scale(0.22));
                    else if (LapidaryScene.overBar) tR = tR.add(0, 0.03, 0);
                    omegaR = 9;
                }
            }
            // toque rapido ao clicar
            double tap = now - tapAt;
            if (tap >= 0 && tap < 0.18) tR = tR.add(0, -0.035 * LapidaryCut.hump(tap / 0.18), 0);
        }

        double w = LapidaryScene.weight;
        if (LapidaryScene.phase == LapidaryScene.Phase.OUTRO) w = Math.min(w, 1 - LapidaryCut.window(now - LapidaryScene.phaseAt, 0, 0.45));
        tR = hidden(true, cam, yaw).add(tR.subtract(hidden(true, cam, yaw)).scale(w));
        tL = hidden(false, cam, yaw).add(tL.subtract(hidden(false, cam, yaw)).scale(w));
        // respiracao
        double b = Math.sin(now * 1.7) * 0.004;
        RIGHT.step(tR.add(up.scale(b)), dt, omegaR);
        LEFT.step(tL.add(up.scale(-b)), dt, omegaL);
    }

    // ---------------------------------------------------------------- desenho
    public static void render(PoseStack ps, MultiBufferSource buf, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        Vec3 cam = camera.getPosition();
        float yaw = camera.getYRot();
        update(cam, yaw);
        int light = LevelRenderer.getLightColor(mc.level, LapidaryScene.pos.above());
        Vec3 f = forward(yaw);
        if (LapidaryScene.phase != LapidaryScene.Phase.OUTRO) {   // na saida os bracos somem (a camera volta e eles esticavam)
            arm(ps, buf, cam, RIGHT.pos, shoulder(true, cam, yaw), true, light, f, player);
            arm(ps, buf, cam, LEFT.pos, shoulder(false, cam, yaw), false, light, f, player);
        }

        // item na mao direita: o que esta sendo carregado, ou a gema sendo levada
        ItemStack held = LapidaryScene.carried();
        if (LapidaryScene.cutting()) {
            double t = LapidaryScene.clock() - LapidaryScene.cutAt;
            held = t >= LapidaryCut.pickAt(LapidaryScene.cutAll) && t < LapidaryCut.placeAt(LapidaryScene.cutAll)
                    ? LapidaryRemote.gemOf(LapidaryScene.cutOre) : ItemStack.EMPTY;
        }
        if (!held.isEmpty() && LapidaryScene.phase != LapidaryScene.Phase.OUTRO) {
            Vec3 dir = RIGHT.pos.subtract(shoulder(true, cam, yaw)).normalize();
            item(ps, buf, held, RIGHT.pos.add(dir.scale(0.035)).add(0, -0.025, 0), cam, yaw, 0.42f, light);
        }

        // gemas recolhidas da mesa: voam da mesa ate a camera
        if (!LapidaryScene.flying.isEmpty()) {
            double nowT = LapidaryScene.clock();
            Vec3 to = cam.add(f.scale(0.3)).add(0, -0.35, 0);
            LapidaryScene.flying.removeIf(fl -> nowT - fl.start() > LapidaryScene.FLY);
            for (LapidaryScene.Flying fl : LapidaryScene.flying) {
                double u = LapidaryCut.window(nowT - fl.start(), 0, LapidaryScene.FLY);
                double k = u * u;
                Vec3 at = fl.from().add(to.subtract(fl.from()).scale(k)).add(0, 0.14 * LapidaryCut.hump(u), 0);
                item(ps, buf, fl.stack(), at, cam, yaw, 0.19f * 1.6f * (float) (1 - 0.6 * u), light);
            }
        }

        // itens indo da barra para a mesa (na mao)
        if (!LapidaryScene.placing.isEmpty()) {
            double nowT = LapidaryScene.clock();
            LapidaryScene.placing.removeIf(pl -> nowT - pl.start() > LapidaryScene.PLACE);
            for (LapidaryScene.Placing pl : LapidaryScene.placing)
                item(ps, buf, pl.stack(), placingAt(pl, cam, yaw, nowT), cam, yaw, 0.3f, light);
        }

        // itens voltando para o inventario ao sair
        if (LapidaryScene.phase == LapidaryScene.Phase.OUTRO) {
            double u = LapidaryCut.window(LapidaryScene.clock() - LapidaryScene.phaseAt, 0, LapidaryScene.OUTRO * 0.8);
            double k = u * u * u;
            Vec3 to = cam.add(f.scale(0.3)).add(0, -0.35, 0);
            for (LapidaryScene.Leaving l : LapidaryScene.leavingItems) {
                if (u >= 1) break;
                Vec3 at = l.from().add(to.subtract(l.from()).scale(k)).add(0, 0.12 * LapidaryCut.hump(u), 0);
                item(ps, buf, l.stack(), at, cam, yaw, l.size() * 1.6f * (float) (1 - 0.7 * u), light);
            }
        }
    }

    // onde esta o item que vai da barra (perto da camera, no alto) ate a mesa
    static Vec3 placingAt(LapidaryScene.Placing pl, Vec3 cam, float yaw, double now) {
        double u = LapidaryCut.inOutCubic(LapidaryCut.window(now - pl.start(), 0, LapidaryScene.PLACE));
        Vec3 from = cam.add(forward(yaw).scale(0.45)).add(0, 0.12, 0);
        return from.add(pl.to().subtract(from).scale(u)).add(0, 0.08 * LapidaryCut.hump(u), 0);
    }

    public static void item(PoseStack ps, MultiBufferSource buf, ItemStack stack, Vec3 at, Vec3 cam, float yaw, float scale, int light) {
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-25f));
        ps.scale(scale, scale, scale);
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        ps.popPose();
    }

    // braco com a skin: o modelo do braco (12 px de comprimento, para +y) e' girado para apontar do ombro ate a mao
    // e esticado ate o comprimento certo
    public static void arm(PoseStack ps, MultiBufferSource buf, Vec3 cam, Vec3 hand, Vec3 shoulder, boolean right, int light,
                            Vec3 forward, LocalPlayer player) {
        arm(ps, buf, cam, hand, shoulder, right, light, forward, player, ARM_THICK);
    }

    // mesma coisa com a grossura escolhida (1 = braco normal do jogo); usado tambem pelos golpes do mod Combat
    public static void arm(PoseStack ps, MultiBufferSource buf, Vec3 cam, Vec3 hand, Vec3 shoulder, boolean right, int light,
                            Vec3 forward, LocalPlayer player, float thick) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        Vec3 d = hand.subtract(shoulder);
        if (d.lengthSqr() < 1e-8) return;
        double length = d.length() + 0.12;
        d = d.normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 upPerp = worldUp.subtract(d.scale(worldUp.dot(d)));
        if (upPerp.lengthSqr() < 1e-6) upPerp = forward;
        upPerp = upPerp.normalize();
        Vec3 x = right ? upPerp.scale(-1) : upPerp;
        Vec3 z = x.cross(d);
        Quaternionf rot = new Matrix3f(
                new Vector3f((float) x.x, (float) x.y, (float) x.z),
                new Vector3f((float) d.x, (float) d.y, (float) d.z),
                new Vector3f((float) z.x, (float) z.y, (float) z.z)).getNormalizedRotation(new Quaternionf());
        ps.pushPose();
        ps.translate(shoulder.x - cam.x, shoulder.y - cam.y, shoulder.z - cam.z);
        ps.mulPose(rot);
        ps.scale(thick, (float) (length * 16.0 / 12.0), thick);
        ps.translate((right ? 5 : -5) / 16f, -0.03f, 0);   // desfaz o pivo do braco no modelo (ombro em x = +-5 px)
        if (right) renderer.renderRightHand(ps, buf, light, player);
        else renderer.renderLeftHand(ps, buf, light, player);
        ps.popPose();
    }
}
