package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.FirstPersonBody;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// corpo da primeira pessoa: sem os itens nas maos (os bracos nao aparecem; as maos sao as da primeira pessoa)
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerBodyMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void binosmovement$hideItems(CallbackInfo ci) {
        if (FirstPersonBody.rendering()) ci.cancel();
    }
}
