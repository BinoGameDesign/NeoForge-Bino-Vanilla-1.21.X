package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.inventory.BackSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Comecar a planar com a elytra das costas (lado do servidor)
@Mixin(Player.class)
public abstract class PlayerElytraMixin {
    @Redirect(method = "tryToStartFallFlying", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack binosvanillaplus$backElytra(Player self, EquipmentSlot slot) {
        return BackSlot.flightStack(self, slot, self.getItemBySlot(slot));
    }
}
