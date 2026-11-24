package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.debug.FreezeWatch;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Vigia de congelamento no mundo de um jogador: o servidor "bate" a cada volta, inclusive pausado (quando pausado o
// jogo salva o mundo: e' justamente ai que o "Salvando mundo" infinito acontece).
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerHeartbeatMixin {
    @Inject(method = "tickServer", at = @At("RETURN"), require = 0)
    private void binosvanillaplus$beat(CallbackInfo ci) { FreezeWatch.beat(); }
}
