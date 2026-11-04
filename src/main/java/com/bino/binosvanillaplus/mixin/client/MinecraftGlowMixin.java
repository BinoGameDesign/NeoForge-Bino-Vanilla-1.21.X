package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.gear.ClientGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Silhueta branca do apito e do sonar: liga o contorno do jogo so no cliente, por alguns segundos
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void binosvanillaplus$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientGlow.has(entity)) cir.setReturnValue(true);
    }
}
