package com.bino.binosvanillaplus.client.bench;

import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

// Bracos da cena da bancada / bigorna (mesmo desenho dos outros). Em descanso, nas laterais do bloco; a direita vai
// ate o que esta sob o mouse, leva/traz itens e, na bigorna, segura um martelo (a maca do jogo) e bate duas vezes.
final class BenchHands {
    private BenchHands() {}

    private static final LapidaryHands.Spring RIGHT = new LapidaryHands.Spring(), LEFT = new LapidaryHands.Spring();
    private static double tapAt = -100, lastFrame;
    private static ItemStack hammer = ItemStack.EMPTY;   // criado so na hora (nunca no carregamento da classe)

    static void begin() { RIGHT.init = LEFT.init = false; lastFrame = BenchScene.clock(); }

    static void tap() { tapAt = BenchScene.clock(); }

    static void render(PoseStack ps, MultiBufferSource buf, Vec3 cam, float yaw, int light) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        double now = BenchScene.clock();
        double dt = Mth.clamp(now - lastFrame, 0, 0.1);
        lastFrame = now;
        Vec3 towardCam = cam.subtract(BenchScene.center).multiply(1, 0, 1).normalize();

        Vec3 tL = BenchScene.local(-0.62, BenchScene.TOP - 0.06, 0.3);
        Vec3 tR = BenchScene.local(0.62, BenchScene.TOP - 0.04, 0.32);
        double omegaR = 15, omegaL = 15;
        BenchScene.Flying grab = BenchScene.flying.isEmpty() ? null : BenchScene.flying.get(BenchScene.flying.size() - 1);
        BenchScene.Placing put = BenchScene.placing.isEmpty() ? null : BenchScene.placing.get(BenchScene.placing.size() - 1);
        double strike = BenchScene.strikeAt > 0 ? now - BenchScene.strikeAt : -1;

        if (BenchScene.phase == BenchScene.Phase.ACTIVE) {
            if (strike >= 0) {
                // martelada: sobe, desce na bigorna, sobe de novo e desce (as batidas coincidem com as faiscas)
                Vec3 hit = BenchScene.local(0.05, BenchScene.TOP + 0.08, 0.06);
                double s = strike / BenchScene.STRIKE;
                double up = s < 0.42 ? LapidaryCut.hump(s / 0.42) : s < 0.82 ? LapidaryCut.hump((s - 0.42) / 0.40) : 0;
                tR = hit.add(0, 0.28 * up, 0).add(towardCam.scale(0.06 * up));
                omegaR = 34;
                tL = BenchScene.local(-0.3, BenchScene.TOP + 0.03, 0.16);   // esquerda segura a peca
            } else if (put != null) {
                tR = BenchScene.placingAt(put, cam, yaw, now).add(0, 0.03, 0).add(towardCam.scale(0.04));
                omegaR = 28;
            } else if (grab != null && now - grab.start() < BenchScene.FLY) {
                double u = now - grab.start();
                tR = u < 0.1 ? grab.from().add(0, 0.04, 0) : BenchScene.flyingAt(grab, cam, yaw, now).add(0, 0.03, 0);
                omegaR = 30;
            } else if (BenchScene.hover >= 0) {
                Vec3 p = BenchScene.hover == BenchScene.resultSlot() ? BenchScene.resultPoint().add(0, -0.08, 0) : BenchScene.slotPoint(BenchScene.hover).add(0, 0.06, 0);
                tR = p.add(towardCam.scale(0.1));
                omegaR = 17;
            } else if (BenchScene.overBar) {
                tR = tR.add(0, 0.04, 0);
            } else {
                double[] f = BenchScene.onTop();
                if (f != null) tR = tR.add(BenchScene.local(Mth.clamp(f[0], -0.4, 0.4), BenchScene.TOP + 0.08, Mth.clamp(f[1], -0.3, 0.35)).subtract(tR).scale(0.2));
                omegaR = 9;
            }
            double tap = now - tapAt;
            if (tap >= 0 && tap < 0.18 && strike < 0) tR = tR.add(0, -0.035 * LapidaryCut.hump(tap / 0.18), 0);
        }

        double w = BenchScene.weight;
        if (BenchScene.phase == BenchScene.Phase.OUTRO) w = Math.min(w, 1 - LapidaryCut.window(now - BenchScene.phaseAt, 0, 0.45));
        Vec3 hR = LapidaryHands.hidden(true, cam, yaw), hL = LapidaryHands.hidden(false, cam, yaw);
        tR = hR.add(tR.subtract(hR).scale(w));
        tL = hL.add(tL.subtract(hL).scale(w));
        double b = Math.sin(now * 1.7) * 0.004;
        RIGHT.step(tR.add(0, b, 0), dt, omegaR);
        LEFT.step(tL.add(0, -b, 0), dt, omegaL);

        Vec3 f = LapidaryHands.forward(yaw);
        if (w > 0.02 || RIGHT.pos.distanceToSqr(hR) > 0.001) {
            LapidaryHands.arm(ps, buf, cam, RIGHT.pos, LapidaryHands.shoulder(true, cam, yaw), true, light, f, player);
            LapidaryHands.arm(ps, buf, cam, LEFT.pos, LapidaryHands.shoulder(false, cam, yaw), false, light, f, player);
        }
        Vec3 dir = RIGHT.pos.subtract(LapidaryHands.shoulder(true, cam, yaw)).normalize();
        if (strike >= 0) {
            // martelo na mao: cabo seguindo o braco, cabeca para baixo
            Minecraft mc = Minecraft.getInstance();
            Vec3 at = RIGHT.pos.add(dir.scale(0.05));
            ps.pushPose();
            ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
            ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            ps.mulPose(Axis.XP.rotationDegrees(-70f));
            ps.mulPose(Axis.ZP.rotationDegrees(-45f));
            ps.scale(0.5f, 0.5f, 0.5f);
            if (hammer.isEmpty()) hammer = new ItemStack(Items.MACE);
            mc.getItemRenderer().renderStatic(hammer, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
            ps.popPose();
        } else {
            ItemStack held = BenchScene.carried();
            if (!held.isEmpty() && BenchScene.phase != BenchScene.Phase.OUTRO)
                LapidaryHands.item(ps, buf, held, RIGHT.pos.add(dir.scale(0.035)).add(0, -0.025, 0), cam, yaw, 0.36f, light);
        }
    }
}
