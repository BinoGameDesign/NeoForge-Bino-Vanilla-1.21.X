package com.bino.binosmining.item;

import com.bino.binosmining.BinosMining;
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
import com.bino.binosmining.item.MiningItems;

// "Material" de armadura: define defesa, resistência, som e a imagem usada no corpo do jogador.
// Balanceamento (defesa por peca: capacete / peitoral / calca / botas; o teto do jogo e 20 pontos no total):
//   Diamante vanilla: 3/6/8/3 (20), resistencia 2, durabilidade x33, encantamento 10, mais +15% de velocidade no set.
//   Jade   : 3/6/8/3 (20), resistencia 2,5, durabilidade x36, encantamento 12  -> a melhor em status brutos
//   Safira : 3/6/8/3 (20), resistencia 2,  durabilidade x33, encantamento 10   -> igual ao diamante sem o bonus do set
//   Rubi   : 3/5/7/3 (18), resistencia 1,  durabilidade x30, encantamento 8    -> a mais fraca
// A durabilidade (x30, x33, x36) e definida em ModItems; o resto aqui.
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BinosMining.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RUBI = register("rubi", () -> Ingredient.of(MiningItems.RUBI),
            3, 5, 7, 3, 10, 8, 1.0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SAPPHIRE = register("sapphire", () -> Ingredient.of(MiningItems.SAPPHIRE),
            3, 6, 8, 3, 11, 10, 2.0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JADE = register("jade", () -> Ingredient.of(MiningItems.JADE),
            3, 6, 8, 3, 11, 12, 2.5f);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, Supplier<Ingredient> repairItem,
                                                                          int helmet, int chest, int legs, int boots, int body,
                                                                          int enchantability, float toughness) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                // Pontos de defesa por peça
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, boots);
                    map.put(ArmorItem.Type.LEGGINGS, legs);
                    map.put(ArmorItem.Type.CHESTPLATE, chest);
                    map.put(ArmorItem.Type.HELMET, helmet);
                    map.put(ArmorItem.Type.BODY, body);   // armadura de cavalo
                }),
                enchantability,                          // encantabilidade (diamante = 10)
                SoundEvents.ARMOR_EQUIP_DIAMOND,         // som ao equipar
                repairItem,                              // com o que se conserta na bigorna
                // Imagens usadas no corpo: textures/models/armor/<nome>_layer_1.png e _layer_2.png
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, name))),
                toughness,                               // resistência (toughness; diamante = 2)
                0.0f));                                  // resistência a empurrão (diamante = 0)
    }

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
