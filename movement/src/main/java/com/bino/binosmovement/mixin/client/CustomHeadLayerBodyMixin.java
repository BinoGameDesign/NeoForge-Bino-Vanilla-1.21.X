package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.FirstPersonBody;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// corpo da primeira pessoa: sem abobora/cabeca de mob na cabeca (a camera fica dentro dela)
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerBodyMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void binosmovement$hideHead(CallbackInfo ci) {
        if (FirstPersonBody.rendering()) ci.cancel();
    }
}
