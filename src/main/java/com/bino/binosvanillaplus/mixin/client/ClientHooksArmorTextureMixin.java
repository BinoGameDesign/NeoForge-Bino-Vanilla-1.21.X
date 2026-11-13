package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.armor.Armor3D;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Pecas de armadura do jogo base com modelo 3D (ex.: capacete de diamante): usa a textura do modelo
// em vez da camada normal do material. As nossas pecas ja fazem isso em Armor3DItem.getArmorTexture.
@Mixin(value = ClientHooks.class, remap = false)
public abstract class ClientHooksArmorTextureMixin {
    @Inject(method = "getArmorTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private static void binosvanillaplus$armor3DTexture(Entity entity, ItemStack armor, ArmorMaterial.Layer layer, boolean innerModel,
                                                         EquipmentSlot slot, CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation tex = Armor3D.texture(armor);
        if (tex != null) cir.setReturnValue(tex);
    }
}
