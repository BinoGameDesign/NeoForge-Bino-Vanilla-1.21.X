package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.gear.ForgeGlow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Brilho de ressonancia da forja (+4 ou mais) no item segurado: primeira e terceira pessoa, jogadores e mobs.
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererForgeGlowMixin {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"), require = 0)
    private void binosvanillaplus$forgeGlow(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                            PoseStack ps, MultiBufferSource buf, int light, CallbackInfo ci) {
        ForgeGlow.renderHeld(entity, stack, ctx, leftHand, ps, buf);
    }
}
