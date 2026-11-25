package com.bino.binoscombat.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Braco com a skin do jogador, do ombro ate a mao (primeira pessoa dos golpes antigos). Copia propria do Combat
// (o Mining tem a mesma coisa nas maos da Mesa de Lapidacao): cada mod leva a sua parte.
final class SkinArm {
    private SkinArm() {}

    // o modelo do braco (12 px de comprimento, para +y) e' girado para apontar do ombro ate a mao e esticado ate o
    // comprimento certo; thick = grossura (1 = braco normal do jogo)
    static void arm(PoseStack ps, MultiBufferSource buf, Vec3 cam, Vec3 hand, Vec3 shoulder, boolean right, int light,
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
