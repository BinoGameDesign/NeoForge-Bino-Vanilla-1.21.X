package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.inventory.BackSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Elytra no slot das costas continua voando (e gastando durabilidade) mesmo com peitoral
@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraMixin {
    @Redirect(method = "updateFallFlying", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack binosvanillaplus$backElytra(LivingEntity self, EquipmentSlot slot) {
        return BackSlot.flightStack(self, slot, self.getItemBySlot(slot));
    }
}
