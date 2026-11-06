package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import com.bino.binosvanillaplus.faction.FactionMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// De quem o villager foge: o Pillager civil nao foge dos pillagers, mas foge dos lutadores da vila;
// o villager normal tambem foge dos lutadores pillager novos (golem pillager).
@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerHostilesSensorMixin {
    @Inject(method = "isMatchingEntity", at = @At("HEAD"), cancellable = true)
    private void binos$factionFear(LivingEntity villager, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        Faction side = FactionData.side(other);
        boolean close = other.distanceToSqr(villager) <= 144;
        if (FactionData.isCivilian(villager)) {
            if (side == Faction.PILLAGER) cir.setReturnValue(false);
            else if (other instanceof FactionMob && side == Faction.VILLAGER) cir.setReturnValue(close);
        } else if (other instanceof FactionMob && side == Faction.PILLAGER && !(other instanceof net.minecraft.world.entity.npc.AbstractVillager)) {
            cir.setReturnValue(close);
        }
    }
}
