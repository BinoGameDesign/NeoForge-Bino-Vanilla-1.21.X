package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.HitFx;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mod Combat: o cliente recebeu "essa entidade tomou dano" -> flash branco e recuo (HitFx)
@Mixin(LivingEntity.class)
public abstract class LivingEntityHitFxMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void binosvanillaplus$hitFx(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) HitFx.onHurt(self, source);
    }
}
