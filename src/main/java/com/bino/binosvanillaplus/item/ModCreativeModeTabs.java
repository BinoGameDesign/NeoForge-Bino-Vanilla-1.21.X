package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BinosVanillaMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MINING_ITEMS_TAB = CREATIVE_MODE_TAB.register("mining_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.RUBI.get()))
                    .title(Component.translatable("creativetab.binosvanillaplus.mining_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.RUBI);
                        output.accept(ModItems.RUBI_SWORD);
                        output.accept(ModItems.RUBI_PICKAXE);
                        output.accept(ModItems.RUBI_SHOVEL);
                        output.accept(ModItems.RUBI_AXE);
                        output.accept(ModItems.RUBI_HOE);
                        output.accept(ModItems.RUBI_HELMET);
                        output.accept(ModItems.RUBI_CHESTPLATE);
                        output.accept(ModItems.RUBI_LEGGINGS);
                        output.accept(ModItems.RUBI_BOOTS);
                        output.accept(ModItems.SAPPHIRE);
                        output.accept(ModItems.SAPPHIRE_SWORD);
                        output.accept(ModItems.SAPPHIRE_PICKAXE);
                        output.accept(ModItems.SAPPHIRE_SHOVEL);
                        output.accept(ModItems.SAPPHIRE_AXE);
                        output.accept(ModItems.SAPPHIRE_HOE);
                        output.accept(ModItems.SAPPHIRE_HELMET);
                        output.accept(ModItems.SAPPHIRE_CHESTPLATE);
                        output.accept(ModItems.SAPPHIRE_LEGGINGS);
                        output.accept(ModItems.SAPPHIRE_BOOTS);
                        output.accept(ModItems.JADE);
                        output.accept(ModItems.JADE_SWORD);
                        output.accept(ModItems.JADE_PICKAXE);
                        output.accept(ModItems.JADE_SHOVEL);
                        output.accept(ModItems.JADE_AXE);
                        output.accept(ModItems.JADE_HOE);
                        output.accept(ModItems.JADE_HELMET);
                        output.accept(ModItems.JADE_CHESTPLATE);
                        output.accept(ModItems.JADE_LEGGINGS);
                        output.accept(ModItems.JADE_BOOTS);
                        output.accept(ModItems.FORGE_STONE_1);
                        output.accept(ModItems.FORGE_STONE_2);
                        output.accept(ModItems.FORGE_STONE_3);
                        output.accept(ModItems.FORGE_STONE_4);
                        output.accept(ModItems.FORGE_STONE_5);
                        output.accept(ModItems.FORGE_STONE_6);
                        output.accept(ModItems.FORGE_STONE_7);
                        output.accept(ModItems.FORGE_STONE_8);
                        output.accept(ModItems.FORGE_STONE_9);
                        output.accept(ModItems.FORGE_STONE_10);
                        output.accept(ModItems.PHILOSOPHER_FRAGMENT);
                        output.accept(ModItems.PHILOSOPHER_STONE_INCOMPLETE);
                        output.accept(ModItems.PHILOSOPHER_STONE_SMALL);
                        output.accept(ModItems.PHILOSOPHER_STONE_LARGE);
                        output.accept(ModItems.RAW_DIAMOND);
                        output.accept(ModItems.RAW_RUBI);
                        output.accept(ModItems.RAW_SAPPHIRE);
                        output.accept(ModItems.RAW_JADE);
                    }).build());

    public static final Supplier<CreativeModeTab> MINING_BLOCK_TAB = CREATIVE_MODE_TAB.register("mining_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.RUBI_ORE))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mining_items_tab"))
                    .title(Component.translatable("creativetab.binosvanillaplus.mining_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.RUBI_ORE);
                        output.accept(ModBlocks.RUBI_DEEPSLATE_ORE);
                        output.accept(ModBlocks.RUBI_BLOCK);
                        output.accept(ModBlocks.SAPPHIRE_ORE);
                        output.accept(ModBlocks.SAPPHIRE_DEEPSLATE_ORE);
                        output.accept(ModBlocks.SAPPHIRE_BLOCK);
                        output.accept(ModBlocks.JADE_ORE);
                        output.accept(ModBlocks.JADE_DEEPSLATE_ORE);
                        output.accept(ModBlocks.JADE_BLOCK);
                        output.accept(ModBlocks.FORGE_ORE);
                        output.accept(ModBlocks.PHILOSOPHER_BLOCK);
                        output.accept(ModBlocks.LAPIDARY_TABLE);
                    }).build());

    public static final Supplier<CreativeModeTab> CAVE_EXPLORATION_TAB = CREATIVE_MODE_TAB.register("cave_exploration_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CYALUME_STICK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mining_blocks_tab"))
                    .title(Component.translatable("creativetab.binosvanillaplus.cave_exploration"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CYALUME_STICK);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}