package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.atmosphere.AtmosphereState;
import com.bino.binosvanillaplus.client.map.MapOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Tinge a cor do ceu do jogo com o matiz do bioma (cherry lilas, pantano esverdeado, deserto mais claro...).
// O brilho continua o do jogo (noite, chuva). O Iris passa essa mesma cor como "skyColor" para o shaderpack,
// entao o Complementary tambem recebe o ceu do bioma.
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void binosvanillaplus$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (MapOptions.atmosphere < 1) return;
        Vec3 v = cir.getReturnValue();
        Vec3 t = AtmosphereState.tintSky(v);
        if (t != v) cir.setReturnValue(t);
    }
}
