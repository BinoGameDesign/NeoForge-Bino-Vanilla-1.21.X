package com.bino.binosmining.mixin.client;

import com.bino.binosmining.client.gear.ForgeOutline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Contorno da forja (+4 a +10) em todo item desenhado em 3D: mao, chao, moldura (ver ForgeOutline).
// No HEAD: a pose ainda e' a de quem chamou; o item vai para a mascara do contorno.
@Mixin(ItemRenderer.class)
public abstract class ItemRendererForgeOutlineMixin {
    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"), require = 0)
    private void binosmining$forgeOutline(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps,
                                               MultiBufferSource buf, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (!stack.isEmpty()) ForgeOutline.render(stack, ctx, leftHand, ps, buf, model);
    }
}
