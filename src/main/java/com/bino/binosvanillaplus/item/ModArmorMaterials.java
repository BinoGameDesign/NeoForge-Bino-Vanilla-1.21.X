package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

// "Material" de armadura: define defesa, resistência, som e a imagem usada no corpo do jogador.
// Rubi, Safira e Jade usam os mesmos números da armadura de diamante.
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BinosVanillaMod.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RUBI = register("rubi", () -> Ingredient.of(ModItems.RUBI));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SAPPHIRE = register("sapphire", () -> Ingredient.of(ModItems.SAPPHIRE));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JADE = register("jade", () -> Ingredient.of(ModItems.JADE));

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, Supplier<Ingredient> repairItem) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                // Pontos de defesa por peça (igual ao diamante)
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, 3);
                    map.put(ArmorItem.Type.LEGGINGS, 6);
                    map.put(ArmorItem.Type.CHESTPLATE, 8);
                    map.put(ArmorItem.Type.HELMET, 3);
                    map.put(ArmorItem.Type.BODY, 11);   // armadura de cavalo
                }),
                10,                                      // encantabilidade (diamante = 10)
                SoundEvents.ARMOR_EQUIP_DIAMOND,         // som ao equipar
                repairItem,                              // com o que se conserta na bigorna
                // Imagens usadas no corpo: textures/models/armor/<nome>_layer_1.png e _layer_2.png
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name))),
                2.0f,                                    // resistência (toughness; diamante = 2)
                0.0f));                                  // resistência a empurrão (diamante = 0)
    }

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
