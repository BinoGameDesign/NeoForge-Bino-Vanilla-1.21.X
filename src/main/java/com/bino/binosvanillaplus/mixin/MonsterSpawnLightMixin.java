package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.util.SpawnLight;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A luz dos nossos minerios nao atrapalha monstros de nascer nas cavernas (ver SpawnLight).
@Mixin(Monster.class)
public abstract class MonsterSpawnLightMixin {
    @Inject(method = "isDarkEnoughToSpawn", at = @At("RETURN"), cancellable = true)
    private static void binosvanillaplus$ignoreOreLight(ServerLevelAccessor level, BlockPos pos, RandomSource random,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && SpawnLight.onlyIgnoredLight(level, pos))
            cir.setReturnValue(SpawnLight.darkEnoughIgnoringBlockLight(level, pos, random));
    }
}
