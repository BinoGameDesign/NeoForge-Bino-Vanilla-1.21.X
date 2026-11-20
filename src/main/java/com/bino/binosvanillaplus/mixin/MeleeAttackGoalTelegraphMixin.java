package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.combat.Posture;
import com.bino.binosvanillaplus.combat.Telegraphs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mod Combat: o golpe corpo a corpo do mob contra um JOGADOR ganha preparacao (aviso dourado) e pode ser aparado.
// Quebrado/congelado (postura) o mob nao ataca.
@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalTelegraphMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow protected abstract boolean canPerformAttack(LivingEntity target);
    @Shadow protected abstract void resetAttackCooldown();

    @Unique private long binosvanillaplus$windupStart = -1, binosvanillaplus$hitAt = -1;
    @Unique private boolean binosvanillaplus$red;

    @Inject(method = "checkAndPerformAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void binosvanillaplus$telegraph(LivingEntity target, CallbackInfo ci) {
        if (Posture.busy(this.mob)) { binosvanillaplus$windupStart = -1; ci.cancel(); return; }
        if (!Telegraphs.eligible(target)) { binosvanillaplus$windupStart = -1; return; }
        ServerPlayer p = (ServerPlayer) target;
        long now = this.mob.level().getGameTime();
        if (binosvanillaplus$windupStart < 0) {
            if (!canPerformAttack(target)) return;   // o jogo tambem nao bateria
            int wind = Telegraphs.windup(this.mob);
            // ficha de ataque (CombatAI): ja ha 2 mobs atacando este jogador -> espera a vez (rodeando)
            if (!com.bino.binosvanillaplus.combat.CombatAI.take(p, this.mob, wind + Telegraphs.grace(p))) { ci.cancel(); return; }
            binosvanillaplus$windupStart = now;
            binosvanillaplus$hitAt = now + wind + Telegraphs.grace(p);
            binosvanillaplus$red = Telegraphs.rollRed(this.mob);
            Telegraphs.start(this.mob, p, wind, binosvanillaplus$red);
            ci.cancel();
            return;
        }
        if (now < binosvanillaplus$hitAt) { ci.cancel(); return; }   // segurando o golpe
        long start = binosvanillaplus$windupStart;
        binosvanillaplus$windupStart = -1;
        com.bino.binosvanillaplus.combat.CombatAI.attacked(this.mob, p);   // depois do golpe: recua e devolve a ficha
        if (Telegraphs.tryParry(this.mob, p, start, binosvanillaplus$red)) {   // aparado: nao bate
            resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
            return;
        }
        if (!canPerformAttack(target)) {   // o jogador saiu do alcance: golpe no ar
            resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
        }
        // senao: segue o golpe normal do jogo
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 0)
    private void binosvanillaplus$reset(CallbackInfo ci) { binosvanillaplus$windupStart = -1; }
}
