package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.inventory.BackSlot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Comecar a planar com a elytra das costas (lado do jogador: aperta pular no ar)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerElytraMixin {
    @Redirect(method = "aiStep", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack binosvanillaplus$backElytra(LocalPlayer self, EquipmentSlot slot) {
        return BackSlot.flightStack(self, slot, self.getItemBySlot(slot));
    }
}
