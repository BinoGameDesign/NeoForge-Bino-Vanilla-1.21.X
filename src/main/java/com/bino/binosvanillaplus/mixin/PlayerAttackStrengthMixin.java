package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.combat.CombatEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Sem recarga de ataque com espada/machado: o ritmo vem do combo (duracao dos golpes, estamina), entao todo golpe sai
// com carga cheia (dano, critico e varrida normais). Assim o proximo golpe do combo pode entrar logo depois do acerto.
@Mixin(Player.class)
public abstract class PlayerAttackStrengthMixin {
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true, require = 0)
    private void binosvanillaplus$fullStrength(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (CombatEvents.weapon(((Player) (Object) this).getMainHandItem()) != CombatEvents.Weapon.NONE) cir.setReturnValue(1f);
    }
}
