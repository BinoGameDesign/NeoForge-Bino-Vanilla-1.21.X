package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.FirstPersonBody;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Primeira pessoa com o corpo visivel: na lista de entidades do mundo o jogo pula o jogador quando a camera nao esta
// "solta" (camera.isDetached()). Aqui ele passa a desenhar o jogador local tambem (FirstPersonBody esconde a cabeca).
@Mixin(LevelRenderer.class)
public abstract class LevelRendererBodyMixin {
    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"), require = 0)
    private boolean binosmovement$drawOwnBody(boolean detached) {
        return detached || FirstPersonBody.active();
    }
}
