package com.bino.binosvanillaplus.client.anim;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.combat.CombatMoves;
import com.bino.binosvanillaplus.client.combat.DodgeRoll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashSet;
import java.util.Set;

// Vida no corpo do jogador em terceira pessoa (voce no F5 e os outros jogadores), por cima da animacao do jogo:
//  - andando: o tronco torce com a passada, os bracos abrem um pouco e o corpo sobe e desce
//  - correndo: corpo inclinado para a frente, bracos balancando mais forte, passada mais pesada
//  - parado: respiracao (o peso muda de um lado para o outro de leve)
//  - no ar: uma perna a frente e outra atras; caindo, os bracos abrem para equilibrar
//  - em combate com espada/machado: guarda (arma a frente, pernas afastadas, joelhos um pouco dobrados)
// Os golpes e o rolamento (CombatMoves, DodgeRoll) mandam por cima disso.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class PlayerLife {
    private PlayerLife() {}

    private static final Set<Integer> PUSHED = new HashSet<>();

    private static boolean skip(Player p) {
        return p.isSleeping() || p.isPassenger() || p.isFallFlying() || p.isVisuallySwimming() || p.isSwimming()
                || p.getAbilities().flying || p.isSpectator() || p.isAutoSpinAttack();
    }

    public static void pose(HumanoidModel<?> m, Player p, float limbSwing, float amount, float age) {
        // o jogo NUNCA zera a inclinacao lateral do tronco (body.zRot): antes a respiracao somava nela a cada quadro
        // e o tronco ia girando sozinho, separado das pernas. Agora zera aqui e so define (nao soma).
        m.body.zRot = 0f;
        if (skip(p) || m.crouching) return;
        float amt = Math.min(1f, amount);
        float phase = limbSwing * 0.6662f;
        boolean ground = p.onGround();
        boolean busyArms = p.isUsingItem() || m.attackTime > 0;
        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;

        if (ground) {
            // tronco torce com a passada (ombro oposto a perna)
            m.body.yRot += Mth.sin(phase) * 0.12f * amt;
            if (!busyArms) {
                m.rightArm.zRot += 0.05f + 0.06f * amt;
                m.leftArm.zRot -= 0.05f + 0.06f * amt;
            }
            if (p.isSprinting()) {   // correndo: inclinado, bracos fortes
                // (a inclinacao da corrida e' do corpo todo, em onRenderPre: so o tronco separava das pernas)
                if (!busyArms) {
                    m.rightArm.xRot += Mth.cos(phase + Mth.PI) * 0.45f * amt - 0.2f * amt;
                    m.leftArm.xRot += Mth.cos(phase) * 0.45f * amt - 0.2f * amt;
                }
                m.rightLeg.xRot *= 1.15f;
                m.leftLeg.xRot *= 1.15f;
            }
            if (amt < 0.15f) {   // parado: respiracao
                float b = Mth.sin(age * 0.045f);
                m.body.zRot = b * 0.018f;
            }
        } else if (!p.isInWater()) {   // no ar
            float vy = (float) p.getDeltaMovement().y;
            float fallK = Mth.clamp(-vy * 2.5f, 0f, 1f);
            m.rightLeg.xRot = Mth.lerp(0.7f, m.rightLeg.xRot, -0.55f);
            m.leftLeg.xRot = Mth.lerp(0.7f, m.leftLeg.xRot, 0.3f);
            if (!busyArms) {
                m.rightArm.zRot += 0.15f + 0.35f * fallK;
                m.leftArm.zRot -= 0.15f + 0.35f * fallK;
                m.rightArm.xRot -= 0.25f * fallK;
                m.leftArm.xRot -= 0.25f * fallK;
            }
        }

        // guarda de combate (some quando anda rapido)
        if (ground && !busyArms && CombatMoves.stance(p)) {
            float k = Mth.clamp(1f - amt * 1.6f, 0f, 1f);
            k = k * k * (3 - 2 * k);
            var main = side > 0 ? m.rightArm : m.leftArm;
            var off = side > 0 ? m.leftArm : m.rightArm;
            main.xRot = Mth.lerp(k, main.xRot, -0.62f);
            main.yRot = Mth.lerp(k, main.yRot, -0.32f * side);
            main.zRot = Mth.lerp(k, main.zRot, 0.06f * side);
            off.xRot = Mth.lerp(k, off.xRot, -0.28f);
            off.zRot = Mth.lerp(k, off.zRot, -0.22f * side);
            var front = side > 0 ? m.leftLeg : m.rightLeg;   // pe de tras do lado da arma
            var back = side > 0 ? m.rightLeg : m.leftLeg;
            front.xRot = Mth.lerp(k, front.xRot, -0.28f);
            back.xRot = Mth.lerp(k, back.xRot, 0.24f);
            front.zRot = Mth.lerp(k, front.zRot, -0.07f * side);
            back.zRot = Mth.lerp(k, back.zRot, 0.07f * side);
            m.body.yRot = Mth.lerp(k, m.body.yRot, 0.12f * side);
        }
        // ombros acompanham a torcao do tronco (como o jogo faz no golpe), senao os bracos descolam do corpo
        float by = m.body.yRot;
        m.rightArm.z = Mth.sin(by) * 5f; m.rightArm.x = -Mth.cos(by) * 5f;
        m.leftArm.z = -Mth.sin(by) * 5f; m.leftArm.x = Mth.cos(by) * 5f;
    }

    // corpo sobe e desce com a passada, inclina na corrida, e agacha um pouco na guarda
    @SubscribeEvent
    public static void onRenderPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (skip(p) || p.isCrouching() || CombatMoves.moving(p) || DodgeRoll.rolling(p)) return;
        float pt = e.getPartialTick();
        float amt = Math.min(1f, p.walkAnimation.speed(pt));
        float phase = p.walkAnimation.position(pt) * 0.6662f;
        boolean ground = p.onGround();
        float bob = ground ? -Math.abs(Mth.sin(phase)) * (p.isSprinting() ? 0.07f : 0.04f) * amt : 0f;
        float crouch = ground && CombatMoves.stance(p) ? 0.06f * Mth.clamp(1f - amt * 1.6f, 0f, 1f) : 0f;
        float lean = ground && p.isSprinting() ? 7f * amt : 0f;
        if (Math.abs(bob) < 1e-4 && crouch < 1e-4 && lean < 0.01f) return;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        ps.translate(0, bob - crouch, 0);
        if (lean > 0.01f) {
            float yaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            ps.mulPose(Axis.XP.rotationDegrees(lean));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }
}
