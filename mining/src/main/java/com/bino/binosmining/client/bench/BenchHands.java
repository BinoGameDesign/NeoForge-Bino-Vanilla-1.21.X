package com.bino.binosmining.client.bench;

import com.bino.binosmining.client.lapidary.LapidaryCut;
import com.bino.binosmining.client.lapidary.LapidaryHands;
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
// ate o que esta sob o mouse, leva/traz itens e, na bigorna, pega o material e raspa por cima do item.
final class BenchHands {
    private BenchHands() {}

    private static final LapidaryHands.Spring RIGHT = new LapidaryHands.Spring(), LEFT = new LapidaryHands.Spring();
    private static double tapAt = -100, lastFrame;

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
                // raspada: a mao pega o material na ponta e passa por cima do item; a esquerda segura o item
                tR = BenchScene.scrapePoint(strike).add(0, 0.05, 0).add(towardCam.scale(0.05));
                omegaR = 30;
                tL = BenchScene.local(-0.4, BenchScene.TOP + 0.03, 0.14);
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
        if (BenchScene.phase != BenchScene.Phase.OUTRO && (w > 0.02 || RIGHT.pos.distanceToSqr(hR) > 0.001)) {   // na saida somem
            LapidaryHands.arm(ps, buf, cam, RIGHT.pos, LapidaryHands.shoulder(true, cam, yaw), true, light, f, player);
            LapidaryHands.arm(ps, buf, cam, LEFT.pos, LapidaryHands.shoulder(false, cam, yaw), false, light, f, player);
        }
        // item na mao: na ponta do punho (antes ficava escondido dentro da mao)
        Vec3 dir = RIGHT.pos.subtract(LapidaryHands.shoulder(true, cam, yaw)).normalize();
        ItemStack held = strike >= 0 && strike >= BenchScene.SCRAPE_START * 0.6 ? BenchScene.slot(1) : BenchScene.carried();
        if (!held.isEmpty() && BenchScene.phase != BenchScene.Phase.OUTRO) {
            Vec3 at = RIGHT.pos.add(dir.scale(0.1)).add(0, 0.07, 0).add(towardCam.scale(0.05));
            BenchScene.drawIcon(ps, buf, held, at, cam, yaw, strike >= 0 ? -60f : -25f, 0.2f, BenchScene.boostLight(light));
        }
    }
}
