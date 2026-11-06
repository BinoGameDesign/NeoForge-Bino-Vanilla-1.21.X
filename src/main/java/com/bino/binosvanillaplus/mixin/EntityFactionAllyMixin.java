package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.faction.FactionData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Mesma faccao = aliados para o jogo todo: a mira dos mobs pula aliados (em vez de escolher o aliado mais perto
// e travar), as presas do evocador nao machucam aliados etc.
@Mixin(Entity.class)
public abstract class EntityFactionAllyMixin {
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void binos$factionAlly(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (other != null && !self.level().isClientSide() && FactionData.allied(self, other)) cir.setReturnValue(true);
    }
}
