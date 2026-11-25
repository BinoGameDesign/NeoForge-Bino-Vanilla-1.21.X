package com.bino.binoscombat.mixin.client;

import com.bino.binoscombat.client.combat.CombatFx;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mod Combat: mob com animacao de morte propria (CombatFx) nao faz a queda de lado do jogo
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererDeathMixin {
    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true, require = 0)
    private void binoscombat$noVanillaFall(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (CombatFx.customDeath(entity)) cir.setReturnValue(0f);
    }
}
