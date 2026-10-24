package com.bino.binosvanillaplus.datagen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BinosVanillaMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Item comum: usa o modelo "generated" (textura plana)
        basicItem(ModItems.RUBI.get());
        handheldItem(ModItems.CYALUME_STICK.get());

        // Ferramentas: usam o modelo "handheld" (seguradas na mão)
        handheldItem(ModItems.RUBI_SWORD.get());
        handheldItem(ModItems.RUBI_PICKAXE.get());
        handheldItem(ModItems.RUBI_SHOVEL.get());
        handheldItem(ModItems.RUBI_AXE.get());
        handheldItem(ModItems.RUBI_HOE.get());

        // ===== Safira e Jade =====
        basicItem(ModItems.SAPPHIRE.get());
        handheldItem(ModItems.SAPPHIRE_SWORD.get());
        handheldItem(ModItems.SAPPHIRE_PICKAXE.get());
        handheldItem(ModItems.SAPPHIRE_SHOVEL.get());
        handheldItem(ModItems.SAPPHIRE_AXE.get());
        handheldItem(ModItems.SAPPHIRE_HOE.get());
        basicItem(ModItems.JADE.get());
        handheldItem(ModItems.JADE_SWORD.get());
        handheldItem(ModItems.JADE_PICKAXE.get());
        handheldItem(ModItems.JADE_SHOVEL.get());
        handheldItem(ModItems.JADE_AXE.get());
        handheldItem(ModItems.JADE_HOE.get());

        // ===== Armaduras =====
        basicItem(ModItems.RUBI_HELMET.get());
        basicItem(ModItems.RUBI_CHESTPLATE.get());
        basicItem(ModItems.RUBI_LEGGINGS.get());
        basicItem(ModItems.RUBI_BOOTS.get());
        basicItem(ModItems.SAPPHIRE_HELMET.get());
        basicItem(ModItems.SAPPHIRE_CHESTPLATE.get());
        basicItem(ModItems.SAPPHIRE_LEGGINGS.get());
        basicItem(ModItems.SAPPHIRE_BOOTS.get());
        basicItem(ModItems.JADE_HELMET.get());
        basicItem(ModItems.JADE_CHESTPLATE.get());
        basicItem(ModItems.JADE_LEGGINGS.get());
        basicItem(ModItems.JADE_BOOTS.get());

        // ===== Forja =====
        basicItem(ModItems.FORGE_STONE_1.get());
        basicItem(ModItems.FORGE_STONE_2.get());
        basicItem(ModItems.FORGE_STONE_3.get());
        basicItem(ModItems.FORGE_STONE_4.get());
        basicItem(ModItems.FORGE_STONE_5.get());
        basicItem(ModItems.FORGE_STONE_6.get());
        basicItem(ModItems.FORGE_STONE_7.get());
        basicItem(ModItems.FORGE_STONE_8.get());
        basicItem(ModItems.FORGE_STONE_9.get());
        basicItem(ModItems.FORGE_STONE_10.get());
        basicItem(ModItems.PHILOSOPHER_FRAGMENT.get());
        basicItem(ModItems.PHILOSOPHER_STONE_INCOMPLETE.get());
        basicItem(ModItems.PHILOSOPHER_STONE_SMALL.get());
        basicItem(ModItems.PHILOSOPHER_STONE_LARGE.get());

        // ===== Minérios brutos =====
        basicItem(ModItems.RAW_DIAMOND.get());
        basicItem(ModItems.RAW_RUBI.get());
        basicItem(ModItems.RAW_SAPPHIRE.get());
        basicItem(ModItems.RAW_JADE.get());
    }
}