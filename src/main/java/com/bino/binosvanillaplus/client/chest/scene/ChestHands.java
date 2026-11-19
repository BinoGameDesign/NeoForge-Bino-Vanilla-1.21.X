package com.bino.binosvanillaplus.client.chest.scene;

import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Bracos da cena do bau (mesmo desenho dos da Mesa de Lapidacao: skin do jogador, mola suave).
// Esquerda: segura a borda da frente do bau. Direita: segue o mouse dentro do bau / sobe ate a barra / leva o item.
// Se o bau era um mimico, as duas maos fogem na hora.
final class ChestHands {
    private ChestHands() {}

    private static final LapidaryHands.Spring RIGHT = new LapidaryHands.Spring(), LEFT = new LapidaryHands.Spring();
    private static double tapAt = -100, lastFrame;

    static void begin() {
        RIGHT.init = LEFT.init = false;
        lastFrame = ChestScene.clock();
    }

    static void tap() { tapAt = ChestScene.clock(); }

    static void render(PoseStack ps, MultiBufferSource buf, Vec3 cam, float yaw, int light) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        double now = ChestScene.clock();
        double dt = Mth.clamp(now - lastFrame, 0, 0.1);
        lastFrame = now;
        double hw = ChestScene.halfWidth();
        Vec3 towardCam = cam.subtract(ChestScene.center).multiply(1, 0, 1).normalize();

        // descanso: as maos seguram as laterais do bau (fora do caminho dos itens)
        Vec3 tL = ChestScene.local(-(hw + 0.14), 0.5, 0.22);
        Vec3 tR = ChestScene.local(hw + 0.14, 0.52, 0.26);
        double omegaR = 15;
        ChestScene.Flying grab = ChestScene.flying.isEmpty() ? null : ChestScene.flying.get(ChestScene.flying.size() - 1);
        ChestScene.Placing put = ChestScene.placing.isEmpty() ? null : ChestScene.placing.get(ChestScene.placing.size() - 1);
        if (ChestScene.phase == ChestScene.Phase.ACTIVE && put != null) {
            tR = ChestScene.placingAt(put, cam, yaw, now).add(0, 0.03, 0).add(towardCam.scale(0.04));   // leva o item
            omegaR = 28;
        } else if (ChestScene.phase == ChestScene.Phase.ACTIVE && grab != null && now - grab.start() < ChestScene.FLY) {
            // pega: vai ate o item e volta com ele (o item voa junto ate a camera)
            double u = now - grab.start();
            tR = u < 0.1 ? grab.from().add(0, 0.04, 0) : ChestScene.flyingAt(grab, cam, yaw, now).add(0, 0.03, 0);
            omegaR = 30;
        } else if (ChestScene.phase == ChestScene.Phase.ACTIVE) {
            if (ChestScene.overBar) {
                tR = tR.add(0, 0.04, 0);   // mouse na barra: a mao so se prepara (antes ia ate la e cobria tudo)
            } else if (ChestScene.hoverSlot >= 0) {
                // a mao fica logo na frente e um pouco a direita do item (apontando), sem cobrir ele
                tR = ChestScene.cellPoint(ChestScene.hoverSlot).add(0, 0.06, 0).add(towardCam.scale(0.11))
                        .add(ChestScene.local(0.035, 0, 0).subtract(ChestScene.center));
                omegaR = 17;
            } else {
                // sem item sob o mouse: a mao descansa e so "olha" um pouco para o mouse
                double[] f = ChestScene.onFloor();
                if (f != null) tR = tR.add(ChestScene.local(Mth.clamp(f[0], -hw, hw), 0.62, Mth.clamp(f[1], -0.3, 0.4)).subtract(tR).scale(0.2));
                omegaR = 9;
            }
            double tap = now - tapAt;
            if (tap >= 0 && tap < 0.18) tR = tR.add(0, -0.035 * LapidaryCut.hump(tap / 0.18), 0);
        }

        double w = ChestScene.weight;
        if (ChestScene.phase == ChestScene.Phase.OUTRO)
            w = ChestScene.serverClosed ? 0 : Math.min(w, 1 - LapidaryCut.window(now - ChestScene.phaseAt, 0, 0.45));
        Vec3 hR = LapidaryHands.hidden(true, cam, yaw), hL = LapidaryHands.hidden(false, cam, yaw);
        tR = hR.add(tR.subtract(hR).scale(w));
        tL = hL.add(tL.subtract(hL).scale(w));
        double b = Math.sin(now * 1.7) * 0.004;
        boolean flee = ChestScene.serverClosed;
        RIGHT.step(tR.add(0, b, 0), dt, flee ? 30 : omegaR);
        LEFT.step(tL.add(0, -b, 0), dt, flee ? 30 : 15);

        Vec3 f = LapidaryHands.forward(yaw);
        if (ChestScene.phase != ChestScene.Phase.OUTRO && (w > 0.02 || RIGHT.pos.distanceToSqr(hR) > 0.001)) {   // na saida somem
            LapidaryHands.arm(ps, buf, cam, RIGHT.pos, LapidaryHands.shoulder(true, cam, yaw), true, light, f, player);
            LapidaryHands.arm(ps, buf, cam, LEFT.pos, LapidaryHands.shoulder(false, cam, yaw), false, light, f, player);
        }
        ItemStack held = ChestScene.carried();
        if (!held.isEmpty() && ChestScene.phase != ChestScene.Phase.OUTRO) {
            Vec3 dir = RIGHT.pos.subtract(LapidaryHands.shoulder(true, cam, yaw)).normalize();
            LapidaryHands.item(ps, buf, held, RIGHT.pos.add(dir.scale(0.035)).add(0, -0.025, 0), cam, yaw, 0.36f, light);
        }
    }
}
