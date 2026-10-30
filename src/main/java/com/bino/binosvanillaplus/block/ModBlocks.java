package com.bino.binosvanillaplus.block;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.block.custom.HookBlock;
import com.bino.binosvanillaplus.block.custom.LapidaryTableBlock;
import com.bino.binosvanillaplus.block.custom.RopeBlock;
import net.minecraft.world.level.material.PushReaction;
import com.bino.binosvanillaplus.block.custom.VariantOreBlock;
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
            () -> new VariantOreBlock(UniformInt.of(3,7),
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // permite que a luz chegue ao bloco, então os cristais deixam de ficar pretos
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> RUBI_DEEPSLATE_ORE = registerBlock("rubi_deepslate_ore",
            () -> new VariantOreBlock(UniformInt.of(4,8),
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

    // ===== SAPPHIRE =====
    public static final DeferredBlock<Block> SAPPHIRE_ORE = registerBlock("sapphire_ore",
            () -> new VariantOreBlock(UniformInt.of(3,7),
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // deixa a luz chegar aos cristais
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> SAPPHIRE_DEEPSLATE_ORE = registerBlock("sapphire_deepslate_ore",
            () -> new VariantOreBlock(UniformInt.of(4,8),
                    BlockBehaviour.Properties.of()
                            .strength(4.0f, 5.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // deixa a luz chegar aos cristais
                            .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> SAPPHIRE_BLOCK = registerBlock("sapphire_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // ===== JADE =====
    public static final DeferredBlock<Block> JADE_ORE = registerBlock("jade_ore",
            () -> new VariantOreBlock(UniformInt.of(3,7),
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // deixa a luz chegar aos cristais
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> JADE_DEEPSLATE_ORE = registerBlock("jade_deepslate_ore",
            () -> new VariantOreBlock(UniformInt.of(4,8),
                    BlockBehaviour.Properties.of()
                            .strength(4.0f, 5.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // deixa a luz chegar aos cristais
                            .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> JADE_BLOCK = registerBlock("jade_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // Minério de forja: solta a Pedra de Forja +1 (loot table em data/.../loot_table/blocks/forge_ore.json)
    public static final DeferredBlock<Block> FORGE_ORE = registerBlock("forge_ore",
            () -> new VariantOreBlock(UniformInt.of(2,5),
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion() // deixa a luz chegar aos cristais
                            .sound(SoundType.STONE)));

    // Bloco de Pedra Filosofal (9 pedras filosofais grandes)
    public static final DeferredBlock<Block> PHILOSOPHER_BLOCK = registerBlock("philosopher_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)));

    // Mesa de Lapidação (modelo 3D: noOcclusion porque não é um cubo cheio)
    public static final DeferredBlock<Block> LAPIDARY_TABLE = registerBlock("lapidary_table",
            () -> new LapidaryTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Corda e gancho: SEM BlockItem automatico (os itens sao registrados a mao em ModItems)
    public static final DeferredBlock<RopeBlock> ROPE = BLOCKS.register("rope",
            () -> new RopeBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.2f)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<HookBlock> CLIMBING_HOOK = BLOCKS.register("climbing_hook",
            () -> new HookBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .noCollission()   // da para atravessar o gancho (a corda fica logo ao lado/abaixo)
                    .noOcclusion()
                    .sound(SoundType.CHAIN)));

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