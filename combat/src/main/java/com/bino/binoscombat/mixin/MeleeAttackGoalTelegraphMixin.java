package com.bino.binoscombat.mixin;

import com.bino.binoscombat.combat.Posture;
import com.bino.binoscombat.combat.Telegraphs;
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
    @Shadow private int ticksUntilNextAttack;

    @Unique private long binoscombat$windupStart = -1, binoscombat$hitAt = -1;
    @Unique private boolean binoscombat$red, binoscombat$chainPending, binoscombat$quickNext;
    @Unique private int binoscombat$chained;

    @Inject(method = "checkAndPerformAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void binoscombat$telegraph(LivingEntity target, CallbackInfo ci) {
        if (Posture.busy(this.mob)) { binoscombat$windupStart = -1; ci.cancel(); return; }
        if (!Telegraphs.eligible(target)) { binoscombat$windupStart = -1; return; }
        ServerPlayer p = (ServerPlayer) target;
        long now = this.mob.level().getGameTime();
        if (binoscombat$windupStart < 0) {
            if (!canPerformAttack(target)) return;   // o jogo tambem nao bateria
            int wind = binoscombat$quickNext ? 7 : Telegraphs.windup(this.mob);   // 2o golpe da sequencia: rapido
            binoscombat$quickNext = false;
            // ficha de ataque (CombatAI): ja ha 2 mobs atacando este jogador -> espera a vez (rodeando)
            if (!com.bino.binoscombat.combat.CombatAI.take(p, this.mob, wind + Telegraphs.grace(p))) { ci.cancel(); return; }
            binoscombat$windupStart = now;
            binoscombat$hitAt = now + wind + Telegraphs.grace(p);
            binoscombat$red = Telegraphs.rollRed(this.mob);
            Telegraphs.start(this.mob, p, wind, binoscombat$red);
            ci.cancel();
            return;
        }
        if (now < binoscombat$hitAt) { ci.cancel(); return; }   // segurando o golpe
        long start = binoscombat$windupStart;
        binoscombat$windupStart = -1;
        if (Telegraphs.tryParry(this.mob, p, start, binoscombat$red)) {   // aparado: nao bate (e a sequencia acaba)
            binoscombat$endChain(p);
            resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
            return;
        }
        if (!canPerformAttack(target)) {   // o jogador saiu do alcance: golpe no ar
            binoscombat$endChain(p);
            resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
        }
        // senao: segue o golpe normal do jogo. Mob forte as vezes emenda outro golpe logo em seguida (CombatAI.chain)
        if (!ci.isCancelled()) {
            if (com.bino.binoscombat.combat.CombatAI.chain(this.mob, binoscombat$chained)) {
                binoscombat$chained++;
                binoscombat$chainPending = true;
            } else binoscombat$endChain(p);
        }
    }

    @Unique
    private void binoscombat$endChain(ServerPlayer p) {
        binoscombat$chained = 0;
        com.bino.binoscombat.combat.CombatAI.attacked(this.mob, p);   // depois do golpe: recua e devolve a ficha
    }

    // o golpe do jogo zerou a recarga para 1 s: na sequencia, o proximo preparo comeca quase na hora
    @Inject(method = "checkAndPerformAttack", at = @At("RETURN"), require = 0)
    private void binoscombat$chainCooldown(LivingEntity target, CallbackInfo ci) {
        if (!binoscombat$chainPending) return;
        binoscombat$chainPending = false;
        binoscombat$quickNext = true;
        this.ticksUntilNextAttack = Math.min(this.ticksUntilNextAttack, 3);
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 0)
    private void binoscombat$reset(CallbackInfo ci) {
        binoscombat$windupStart = -1; binoscombat$chained = 0; binoscombat$chainPending = false; binoscombat$quickNext = false;
    }
}
