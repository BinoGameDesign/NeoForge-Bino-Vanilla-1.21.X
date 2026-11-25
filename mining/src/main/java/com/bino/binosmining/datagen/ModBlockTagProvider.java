package com.bino.binosmining.datagen;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import com.bino.binosmining.block.MiningBlocks;
import com.bino.binosmining.util.MiningTags;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, BinosMining.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(MiningBlocks.RUBI_ORE.get())
                .add(MiningBlocks.RUBI_DEEPSLATE_ORE.get())
                .add(MiningBlocks.RUBI_BLOCK.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(MiningBlocks.RUBI_ORE.get())
                .add(MiningBlocks.RUBI_DEEPSLATE_ORE.get())
                .add(MiningBlocks.RUBI_BLOCK.get());

        // A ferramenta de rubi minera o mesmo que o diamante
        tag(MiningTags.Blocks.INCORRECT_FOR_RUBI_TOOL)
                .addTag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        // Marca os minérios de rubi como "minério", para a picareta de veio reconhecê-los
        tag(Tags.Blocks.ORES)
                .add(MiningBlocks.RUBI_ORE.get())
                .add(MiningBlocks.RUBI_DEEPSLATE_ORE.get());

        // ===== Safira e Jade: mesmas regras do rubi =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(MiningBlocks.SAPPHIRE_ORE.get())
                .add(MiningBlocks.SAPPHIRE_DEEPSLATE_ORE.get())
                .add(MiningBlocks.SAPPHIRE_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(MiningBlocks.SAPPHIRE_ORE.get())
                .add(MiningBlocks.SAPPHIRE_DEEPSLATE_ORE.get())
                .add(MiningBlocks.SAPPHIRE_BLOCK.get());
        tag(Tags.Blocks.ORES)
                .add(MiningBlocks.SAPPHIRE_ORE.get())
                .add(MiningBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(MiningBlocks.JADE_ORE.get())
                .add(MiningBlocks.JADE_DEEPSLATE_ORE.get())
                .add(MiningBlocks.JADE_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(MiningBlocks.JADE_ORE.get())
                .add(MiningBlocks.JADE_DEEPSLATE_ORE.get())
                .add(MiningBlocks.JADE_BLOCK.get());
        tag(Tags.Blocks.ORES)
                .add(MiningBlocks.JADE_ORE.get())
                .add(MiningBlocks.JADE_DEEPSLATE_ORE.get());

        // ===== Minério de forja: picareta de ferro ou melhor =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MiningBlocks.FORGE_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(MiningBlocks.FORGE_ORE.get());
        tag(Tags.Blocks.ORES).add(MiningBlocks.FORGE_ORE.get());

        // ===== Bloco filosofal (picareta de ferro) e Mesa de Lapidação (machado) =====
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MiningBlocks.PHILOSOPHER_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(MiningBlocks.PHILOSOPHER_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(MiningBlocks.LAPIDARY_TABLE.get());
    }
}