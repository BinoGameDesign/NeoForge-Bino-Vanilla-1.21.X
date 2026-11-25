package com.bino.binoscombat.mixin.client;

import com.bino.binoscombat.client.combat.Stamina;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Estamina normal zerada: sem correr (o jogo usa este teste tanto para comecar quanto para continuar a corrida)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerStaminaMixin {
    @Inject(method = "hasEnoughFoodToStartSprinting", at = @At("HEAD"), cancellable = true, require = 0)
    private void binoscombat$stamina(CallbackInfoReturnable<Boolean> cir) {
        if (!Stamina.canSprint()) cir.setReturnValue(false);
    }
}
