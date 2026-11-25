package com.bino.binoscombat.mixin.client;

import com.bino.binoscombat.client.combat.HitFx;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mod Combat: no instante do impacto a entidade fica BRANCA (a faixa branca do overlay, a mesma do creeper inchando);
// o vermelho normal de dano foi tirado (o Bino preferiu so o flash).
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererFlashMixin {
    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void binoscombat$whiteFlash(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (HitFx.flashing(entity)) cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(1.0f), OverlayTexture.v(false)));
        // sem o vermelho do dano normal do jogo (so fica vermelho morrendo); o branco do creeper inchando continua
        else cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.deathTime > 0)));
    }
}
