package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.leveling.PlayerStats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// VONTADE (27/11): fome maior sem mudar a barra (continua com 10 coxinhas). Cada ponto da barra vale
// 1 + 0,02 x Vontade: o cansaco que gasta a fome e o que a comida enche sao divididos por esse numero (as sobras
// quebradas ficam guardadas para a proxima vez). Igual a Vitalidade com os coracoes.
@Mixin(FoodData.class)
public abstract class FoodDataWillMixin {
    @Unique private float binosvanillaplus$scale = 1f;
    @Unique private float binosvanillaplus$foodRest = 0f;

    @Inject(method = "tick", at = @At("HEAD"))
    private void binosvanillaplus$readWill(Player player, CallbackInfo ci) {
        binosvanillaplus$scale = Math.max(1f, PlayerStats.willScale(player));
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float binosvanillaplus$lessExhaustion(float exhaustion) {
        return exhaustion / binosvanillaplus$scale;
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int binosvanillaplus$lessFood(int food) {
        if (binosvanillaplus$scale <= 1.0001f || food <= 0) return food;
        float f = food / binosvanillaplus$scale + binosvanillaplus$foodRest;
        int whole = (int) f;
        binosvanillaplus$foodRest = f - whole;
        return whole;
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float binosvanillaplus$lessSaturation(float saturation) {
        return saturation / binosvanillaplus$scale;
    }
}
