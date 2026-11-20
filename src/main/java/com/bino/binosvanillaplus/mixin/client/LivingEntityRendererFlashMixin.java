package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.HitFx;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mod Combat: no instante do impacto a entidade fica BRANCA (a faixa branca do overlay, a mesma do creeper inchando);
// passado o flash, volta o vermelho normal de dano.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererFlashMixin {
    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void binosvanillaplus$whiteFlash(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (HitFx.flashing(entity)) cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(1.0f), OverlayTexture.v(false)));
    }
}
