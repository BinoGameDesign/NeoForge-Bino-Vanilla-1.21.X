package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.combat.Posture;
import com.bino.binosvanillaplus.combat.Telegraphs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mod Combat: o slime (e o cubo de magma) nao tem golpe, ele machuca ENCOSTANDO. Agora, ao encostar num jogador, ele
// "prepara" (aviso do parry por 0,45 s) e so entao machuca; aparado, ele e' rebatido e nao machuca.
@Mixin(Slime.class)
public abstract class SlimeTelegraphMixin {
    @Unique private long binosvanillaplus$hitAt = -1;

    @Inject(method = "dealDamage", at = @At("HEAD"), cancellable = true, require = 0)
    private void binosvanillaplus$telegraph(LivingEntity target, CallbackInfo ci) {
        Slime self = (Slime) (Object) this;
        if (Posture.busy(self)) { ci.cancel(); return; }
        if (!Telegraphs.eligible(target)) return;
        long now = self.level().getGameTime();
        if (binosvanillaplus$hitAt >= 0 && now > binosvanillaplus$hitAt + 10) binosvanillaplus$hitAt = -1;   // saiu de perto: aviso venceu
        if (binosvanillaplus$hitAt < 0) {
            int wind = 9;
            binosvanillaplus$hitAt = now + wind + Telegraphs.grace((net.minecraft.server.level.ServerPlayer) target);
            Telegraphs.startCustom(self, target, wind);
            ci.cancel();
            return;
        }
        if (now < binosvanillaplus$hitAt) { ci.cancel(); return; }   // preparando
        binosvanillaplus$hitAt = -1;
        if (Telegraphs.parriedCustom(self, target)) ci.cancel();   // aparado: nao machuca
        // senao: machuca como o jogo
    }
}
