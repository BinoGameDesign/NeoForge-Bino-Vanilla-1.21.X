package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.MiningStages;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Progresso de quebra (a rachadura) -> fases dos minerios (os cristais vao caindo)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
    @Inject(method = "destroyBlockProgress", at = @At("HEAD"), require = 0)
    private void binosvanillaplus$miningStage(int breakerId, BlockPos pos, int progress, CallbackInfo ci) {
        MiningStages.onProgress(breakerId, pos, progress);
    }
}
