package com.bino.binosmining.datagen;

import com.bino.binosmining.BinosMining;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import com.bino.binosmining.item.MiningItems;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BinosMining.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Item comum: usa o modelo "generated" (textura plana)
        basicItem(MiningItems.RUBI.get());

        // Ferramentas: usam o modelo "handheld" (seguradas na mão)
        handheldItem(MiningItems.RUBI_SWORD.get());
        handheldItem(MiningItems.RUBI_PICKAXE.get());
        handheldItem(MiningItems.RUBI_SHOVEL.get());
        handheldItem(MiningItems.RUBI_AXE.get());
        handheldItem(MiningItems.RUBI_HOE.get());

        // ===== Safira e Jade =====
        basicItem(MiningItems.SAPPHIRE.get());
        handheldItem(MiningItems.SAPPHIRE_SWORD.get());
        handheldItem(MiningItems.SAPPHIRE_PICKAXE.get());
        handheldItem(MiningItems.SAPPHIRE_SHOVEL.get());
        handheldItem(MiningItems.SAPPHIRE_AXE.get());
        handheldItem(MiningItems.SAPPHIRE_HOE.get());
        basicItem(MiningItems.JADE.get());
        handheldItem(MiningItems.JADE_SWORD.get());
        handheldItem(MiningItems.JADE_PICKAXE.get());
        handheldItem(MiningItems.JADE_SHOVEL.get());
        handheldItem(MiningItems.JADE_AXE.get());
        handheldItem(MiningItems.JADE_HOE.get());

        // ===== Armaduras =====
        basicItem(MiningItems.RUBI_HELMET.get());
        basicItem(MiningItems.RUBI_CHESTPLATE.get());
        basicItem(MiningItems.RUBI_LEGGINGS.get());
        basicItem(MiningItems.RUBI_BOOTS.get());
        basicItem(MiningItems.SAPPHIRE_HELMET.get());
        basicItem(MiningItems.SAPPHIRE_CHESTPLATE.get());
        basicItem(MiningItems.SAPPHIRE_LEGGINGS.get());
        basicItem(MiningItems.SAPPHIRE_BOOTS.get());
        basicItem(MiningItems.JADE_HELMET.get());
        basicItem(MiningItems.JADE_CHESTPLATE.get());
        basicItem(MiningItems.JADE_LEGGINGS.get());
        basicItem(MiningItems.JADE_BOOTS.get());

        // ===== Forja =====
        basicItem(MiningItems.FORGE_STONE_1.get());
        basicItem(MiningItems.FORGE_STONE_2.get());
        basicItem(MiningItems.FORGE_STONE_3.get());
        basicItem(MiningItems.FORGE_STONE_4.get());
        basicItem(MiningItems.FORGE_STONE_5.get());
        basicItem(MiningItems.FORGE_STONE_6.get());
        basicItem(MiningItems.FORGE_STONE_7.get());
        basicItem(MiningItems.FORGE_STONE_8.get());
        basicItem(MiningItems.FORGE_STONE_9.get());
        basicItem(MiningItems.FORGE_STONE_10.get());
        basicItem(MiningItems.PHILOSOPHER_FRAGMENT.get());
        basicItem(MiningItems.PHILOSOPHER_STONE_INCOMPLETE.get());
        basicItem(MiningItems.PHILOSOPHER_STONE_SMALL.get());
        basicItem(MiningItems.PHILOSOPHER_STONE_LARGE.get());

        // ===== Minérios brutos =====
        basicItem(MiningItems.RAW_DIAMOND.get());
        basicItem(MiningItems.RAW_RUBI.get());
        basicItem(MiningItems.RAW_SAPPHIRE.get());
        basicItem(MiningItems.RAW_JADE.get());
    }
}