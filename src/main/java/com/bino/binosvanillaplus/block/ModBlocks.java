package com.bino.binosvanillaplus.block;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(BinosVanillaMod.MOD_ID);


    public static final DeferredBlock<Block> RUBI_ORE = registerBlock("rubi_ore",
            () -> new DropExperienceBlock(UniformInt.of(3,7),
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // permite que a luz chegue ao bloco, então os cristais deixam de ficar pretos
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> RUBI_DEEPSLATE_ORE = registerBlock("rubi_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(4,8),
                    BlockBehaviour.Properties.of()
                            .strength(4.0f, 5.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // mesma coisa para o minério da deepslate
                            .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> RUBI_BLOCK = registerBlock("rubi_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}