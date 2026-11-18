package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.eat.EatAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Terceira pessoa (o que os outros veem): comendo uma comida com animacao, as duas maos levam a comida a boca e
// ela da mordidinhas (a cada ~0,5 s a mao chega mais perto e a cabeca abaixa um pouco).
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelEatMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void binosvanillaplus$eatPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                          float headPitch, CallbackInfo ci) {
        if (!EatAnimation.eatingAnimated(entity)) return;
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        float t = entity.getTicksUsingItem() + (ageInTicks - (int) ageInTicks);
        float bite = (float) Math.max(0, Math.sin(t * 0.63));              // ~3 mordidas a cada 2 s
        float up = Math.min(1f, t / 6f);                                   // sobe nos primeiros ticks
        boolean rightUses = (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.RIGHT);
        var main = rightUses ? m.rightArm : m.leftArm;
        var other = rightUses ? m.leftArm : m.rightArm;
        float side = rightUses ? 1f : -1f;
        main.xRot = up * (-1.75f - 0.15f * bite) + (1 - up) * main.xRot;
        main.yRot = up * (-0.45f * side);
        main.zRot = 0;
        other.xRot = up * (-1.45f - 0.1f * bite) + (1 - up) * other.xRot;
        other.yRot = up * (0.5f * side);
        other.zRot = 0;
        m.head.xRot += 0.15f * bite;
    }
}
