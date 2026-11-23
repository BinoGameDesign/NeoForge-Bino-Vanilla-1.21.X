package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.CombatMoves;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Terceira pessoa: a arma segue o osso "item" das animacoes do Blockbench (gira e estica a partir da mao).
// Entra logo depois do jogo levar a pose ate a mao (translateToHand), antes dos giros proprios do item.
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerCombatMixin {
    @Inject(method = "renderArmWithItem", require = 0,
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void binosvanillaplus$combatItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, HumanoidArm arm, PoseStack ps,
                                             MultiBufferSource buffers, int light, CallbackInfo ci) {
        CombatMoves.poseItem(entity, arm, ps);
    }
}
