package com.bino.binosmining.mixin.client;

import com.bino.binosmining.client.gear.ForgeOutline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Contorno da forja: o corpo de quem segura um item forjado vira "parede" na mascara do contorno (ForgeOutline),
// para o contorno nao atravessar o personagem. Pega o modelo ja posado, no momento em que o jogo o desenha.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererForgeOutlineMixin {
    @Shadow protected EntityModel<?> model;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 0)
    private void binosmining$forgeOutlineBlocker(LivingEntity entity, float yaw, float partialTick, PoseStack ps,
                                                      MultiBufferSource buf, int light, CallbackInfo ci) {
        if (ForgeOutline.wantsBlocker(entity))
            model.renderToBuffer(ps, ForgeOutline.blocker(buf), light, OverlayTexture.NO_OVERLAY, -1);
    }
}
