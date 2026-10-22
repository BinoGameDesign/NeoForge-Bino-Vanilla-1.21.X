package com.bino.binosvanillaplus.datagen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags,
                              @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, BinosVanillaMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.SWORDS).add(ModItems.RUBI_SWORD.get());
        tag(ItemTags.PICKAXES).add(ModItems.RUBI_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(ModItems.RUBI_SHOVEL.get());
        tag(ItemTags.AXES).add(ModItems.RUBI_AXE.get());
        tag(ItemTags.HOES).add(ModItems.RUBI_HOE.get());

        // Opcional: marca o rubi como gema, para outros mods o reconhecerem
        tag(Tags.Items.GEMS).add(ModItems.RUBI.get());

        // ===== Safira e Jade =====
        tag(ItemTags.SWORDS).add(ModItems.SAPPHIRE_SWORD.get());
        tag(ItemTags.PICKAXES).add(ModItems.SAPPHIRE_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(ModItems.SAPPHIRE_SHOVEL.get());
        tag(ItemTags.AXES).add(ModItems.SAPPHIRE_AXE.get());
        tag(ItemTags.HOES).add(ModItems.SAPPHIRE_HOE.get());
        tag(Tags.Items.GEMS).add(ModItems.SAPPHIRE.get());
        tag(ItemTags.SWORDS).add(ModItems.JADE_SWORD.get());
        tag(ItemTags.PICKAXES).add(ModItems.JADE_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(ModItems.JADE_SHOVEL.get());
        tag(ItemTags.AXES).add(ModItems.JADE_AXE.get());
        tag(ItemTags.HOES).add(ModItems.JADE_HOE.get());
        tag(Tags.Items.GEMS).add(ModItems.JADE.get());
    }
}