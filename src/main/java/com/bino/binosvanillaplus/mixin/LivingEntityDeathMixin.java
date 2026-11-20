package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.combat.CombatEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mod Combat: o mob morto fica 2 s no chao (em vez de 1 s) para a animacao de morte aparecer inteira; some com o "puf".
// Jogadores continuam como o jogo (tela de morte).
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true, require = 0)
    private void binosvanillaplus$longerDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) return;
        ci.cancel();
        self.deathTime++;
        if (self.deathTime >= 40 && !self.level().isClientSide() && !self.isRemoved()) {
            CombatEvents.deathPuff(self);
            self.level().broadcastEntityEvent(self, (byte) 60);
            self.remove(Entity.RemovalReason.KILLED);
        }
    }
}
