package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.util.ArmorModel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Troca o teto de 20 pontos de armadura do vanilla pela curva suave do ArmorModel.
// getDamageAfterAbsorb faz clamp(armor - dano/(2+toughness/4), armor*0.2, 20) e depois divide por 25.
@Mixin(CombatRules.class)
public abstract class CombatRulesArmorMixin {
    @Redirect(method = "getDamageAfterAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    private static float binos$softCap(float value, float min, float max) {
        if (max == 20.0f) return ArmorModel.soften(Math.max(value, min));   // o clamp da armadura (o outro vai de 0 a 1)
        return Mth.clamp(value, min, max);
    }
}
