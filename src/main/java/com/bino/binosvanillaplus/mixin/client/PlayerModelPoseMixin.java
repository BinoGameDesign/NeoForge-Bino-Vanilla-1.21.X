package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.inventory.ActionPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pose de acao do personagem desenhado no inventario novo (so enquanto o menu desenha o jogador)
@Mixin(PlayerModel.class)
public abstract class PlayerModelPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void binosvanillaplus$pose(LivingEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (ActionPose.current != null) ActionPose.apply((PlayerModel<?>) (Object) this, ActionPose.current, age);
    }
}
