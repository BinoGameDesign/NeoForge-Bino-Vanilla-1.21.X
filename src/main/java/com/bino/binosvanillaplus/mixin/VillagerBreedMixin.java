package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.faction.FactionData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// filho de Pillagers civis nasce Pillager civil (pais misturados: metade de chance)
@Mixin(Villager.class)
public abstract class VillagerBreedMixin {
    @Inject(method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/npc/Villager;", at = @At("RETURN"))
    private void binos$civilianChild(ServerLevel level, AgeableMob other, CallbackInfoReturnable<Villager> cir) {
        Villager child = cir.getReturnValue();
        if (child == null) return;
        boolean a = FactionData.isCivilian((Villager) (Object) this), b = FactionData.isCivilian(other);
        if ((a && b) || (a != b && level.random.nextBoolean())) FactionData.setCivilian(child, true);
    }
}
