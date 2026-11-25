package com.bino.binosmining.datagen;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import com.bino.binosmining.item.MiningItems;

public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags,
                              @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, BinosMining.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.SWORDS).add(MiningItems.RUBI_SWORD.get());
        tag(ItemTags.PICKAXES).add(MiningItems.RUBI_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(MiningItems.RUBI_SHOVEL.get());
        tag(ItemTags.AXES).add(MiningItems.RUBI_AXE.get());
        tag(ItemTags.HOES).add(MiningItems.RUBI_HOE.get());

        // Opcional: marca o rubi como gema, para outros mods o reconhecerem
        tag(Tags.Items.GEMS).add(MiningItems.RUBI.get());

        // ===== Safira e Jade =====
        tag(ItemTags.SWORDS).add(MiningItems.SAPPHIRE_SWORD.get());
        tag(ItemTags.PICKAXES).add(MiningItems.SAPPHIRE_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(MiningItems.SAPPHIRE_SHOVEL.get());
        tag(ItemTags.AXES).add(MiningItems.SAPPHIRE_AXE.get());
        tag(ItemTags.HOES).add(MiningItems.SAPPHIRE_HOE.get());
        tag(Tags.Items.GEMS).add(MiningItems.SAPPHIRE.get());
        tag(ItemTags.SWORDS).add(MiningItems.JADE_SWORD.get());
        tag(ItemTags.PICKAXES).add(MiningItems.JADE_PICKAXE.get());
        tag(ItemTags.SHOVELS).add(MiningItems.JADE_SHOVEL.get());
        tag(ItemTags.AXES).add(MiningItems.JADE_AXE.get());
        tag(ItemTags.HOES).add(MiningItems.JADE_HOE.get());
        tag(Tags.Items.GEMS).add(MiningItems.JADE.get());
    }
}