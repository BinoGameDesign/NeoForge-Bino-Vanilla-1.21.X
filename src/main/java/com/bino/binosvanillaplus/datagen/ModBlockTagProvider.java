package com.bino.binosvanillaplus.datagen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, BinosVanillaMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.RUBI_ORE.get())
                .add(ModBlocks.RUBI_DEEPSLATE_ORE.get())
                .add(ModBlocks.RUBI_BLOCK.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.RUBI_ORE.get())
                .add(ModBlocks.RUBI_DEEPSLATE_ORE.get())
                .add(ModBlocks.RUBI_BLOCK.get());

        // A ferramenta de rubi minera o mesmo que o diamante
        tag(ModTags.Blocks.INCORRECT_FOR_RUBI_TOOL)
                .addTag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        // Marca os minérios de rubi como "minério", para a picareta de veio reconhecê-los
        tag(Tags.Blocks.ORES)
                .add(ModBlocks.RUBI_ORE.get())
                .add(ModBlocks.RUBI_DEEPSLATE_ORE.get());

        // ===== Safira e Jade: mesmas regras do rubi =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SAPPHIRE_ORE.get())
                .add(ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get())
                .add(ModBlocks.SAPPHIRE_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.SAPPHIRE_ORE.get())
                .add(ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get())
                .add(ModBlocks.SAPPHIRE_BLOCK.get());
        tag(Tags.Blocks.ORES)
                .add(ModBlocks.SAPPHIRE_ORE.get())
                .add(ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.JADE_ORE.get())
                .add(ModBlocks.JADE_DEEPSLATE_ORE.get())
                .add(ModBlocks.JADE_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.JADE_ORE.get())
                .add(ModBlocks.JADE_DEEPSLATE_ORE.get())
                .add(ModBlocks.JADE_BLOCK.get());
        tag(Tags.Blocks.ORES)
                .add(ModBlocks.JADE_ORE.get())
                .add(ModBlocks.JADE_DEEPSLATE_ORE.get());

        // ===== Minério de forja: picareta de ferro ou melhor =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FORGE_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.FORGE_ORE.get());
        tag(Tags.Blocks.ORES).add(ModBlocks.FORGE_ORE.get());

        // ===== Bloco filosofal (picareta de ferro) e Mesa de Lapidação (machado) =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.PHILOSOPHER_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.PHILOSOPHER_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.LAPIDARY_TABLE.get());
    }
}