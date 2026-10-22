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
    }
}