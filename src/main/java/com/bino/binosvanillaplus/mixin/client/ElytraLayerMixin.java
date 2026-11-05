package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.inventory.BackSlot;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Desenha a elytra das costas por cima do peitoral
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {
    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack binosvanillaplus$backElytra(LivingEntity self, EquipmentSlot slot) {
        return BackSlot.flightStack(self, slot, self.getItemBySlot(slot));
    }
}
