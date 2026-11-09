package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

// Peca de armadura com modelo 3D proprio (client/armor/Armor3D): usa a textura do modelo,
// textures/models/armor/<nome>_3d.png, em vez da camada normal do material. O icone do item continua 2D.
public class Armor3DItem extends ArmorItem {
    private final ResourceLocation texture;

    public Armor3DItem(Holder<ArmorMaterial> material, Type type, Properties props, String model) {
        super(material, type, props);
        this.texture = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/models/armor/" + model + "_3d.png");
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return texture;
    }
}
