package com.bino.binosvanillaplus.block;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.block.custom.HookBlock;
import com.bino.binosvanillaplus.block.custom.RopeBlock;
import net.minecraft.world.level.material.PushReaction;
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


    // Tumulo de quem morreu: inquebravel (some ao pegar tudo / dissipar). Sem item no criativo (o item existe so como icone do mapa).
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.GraveBlock> GRAVE = BLOCKS.register("grave",
            () -> new com.bino.binosvanillaplus.block.custom.GraveBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(s -> 5)
                    .sound(SoundType.STONE)));

    // Luz invisivel da Lanterna (se apaga sozinha quando a lanterna deixa de apontar para ela). Sem item.
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.FlashlightLightBlock> FLASHLIGHT_LIGHT = BLOCKS.register("flashlight_light",
            () -> new com.bino.binosvanillaplus.block.custom.FlashlightLightBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LIGHT)
                    .noLootTable()));

    // Dinamite (explode pelo Detonador, fogo ou redstone; nao quebra minerios)
    // Queijo (Exploration): bloco com 5 pedacos; o item "cheese" e o proprio bloco
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.CheeseBlock> CHEESE = registerBlock("cheese",
            () -> new com.bino.binosvanillaplus.block.custom.CheeseBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAKE)));
    // Caldeirao com leite/queijo (sem item proprio: quebra e devolve o caldeirao)
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.MilkCauldronBlock> MILK_CAULDRON = BLOCKS.register("milk_cauldron",
            () -> new com.bino.binosvanillaplus.block.custom.MilkCauldronBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAULDRON)));

    // Ninho do King Rat (escondido embaixo da cama do rei no covil; inquebravel, invisivel)
    public static final DeferredBlock<com.bino.binosvanillaplus.entity.rat.KingRatNestBlock> KING_RAT_NEST = registerBlock("king_rat_nest",
            () -> new com.bino.binosvanillaplus.entity.rat.KingRatNestBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .noCollission().noOcclusion().pushReaction(PushReaction.BLOCK)));

    // Ratoeira (ate 16 iscas de queijo)
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.RatTrapBlock> RAT_TRAP = registerBlock("rat_trap",
            () -> new com.bino.binosvanillaplus.block.custom.RatTrapBlock(BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY)));

    // Gaiola (captura viva de ratos e passaros)
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.CageBlock> CAGE = registerBlock("cage",
            () -> new com.bino.binosvanillaplus.block.custom.CageBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.DynamiteBlock> DYNAMITE = registerBlock("dynamite",
            () -> new com.bino.binosvanillaplus.block.custom.DynamiteBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    // Tripe da camera (timer de foto)
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.CameraTripodBlock> CAMERA_TRIPOD = registerBlock("camera_tripod",
            () -> new com.bino.binosvanillaplus.block.custom.CameraTripodBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f).noOcclusion().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));

    // Ossos fossilizados (gerados no mundo em fosseis gigantes; tambem servem de decoracao)
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.FossilBoneBlock> FOSSIL_VERTEBRA = registerBlock("fossil_vertebra",
            () -> new com.bino.binosvanillaplus.block.custom.FossilBoneBlock("vertebra", fossil()));
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.FossilBoneBlock> FOSSIL_RIB = registerBlock("fossil_rib",
            () -> new com.bino.binosvanillaplus.block.custom.FossilBoneBlock("rib", fossil()));
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.FossilBoneBlock> FOSSIL_LIMB = registerBlock("fossil_limb",
            () -> new com.bino.binosvanillaplus.block.custom.FossilBoneBlock("limb", fossil()));
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.FossilSkullBlock> FOSSIL_SKULL = registerBlock("fossil_skull",
            () -> new com.bino.binosvanillaplus.block.custom.FossilSkullBlock(fossil()));

    private static BlockBehaviour.Properties fossil() {
        return BlockBehaviour.Properties.of().strength(2.0f, 3.0f).requiresCorrectToolForDrops()
                .sound(SoundType.BONE_BLOCK).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.XYLOPHONE);
    }

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

    // Parede da Dimensao Portatil (chao, paredes e teto da sala): cimento branco, inquebravel, ilumina a sala. Sem item.
    public static final DeferredBlock<Block> POCKET_WALL = BLOCKS.register("pocket_wall",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noLootTable()
                    .lightLevel(state -> 15)
                    .pushReaction(PushReaction.BLOCK)));

    // Porta Dimensional: sem item proprio (quem cria e a Chave Dimensional) e sem drops
    public static final DeferredBlock<com.bino.binosvanillaplus.block.custom.DimensionalDoorBlock> DIMENSIONAL_DOOR =
            BLOCKS.register("dimensional_door",
                    () -> new com.bino.binosvanillaplus.block.custom.DimensionalDoorBlock(BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.BLOCK)));

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