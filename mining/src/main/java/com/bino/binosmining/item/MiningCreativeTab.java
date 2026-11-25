package com.bino.binosmining.item;

import com.bino.binosmining.BinosMining;
import com.bino.binosmining.block.MiningBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Aba do Mining no modo criativo
public class MiningCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BinosMining.MOD_ID);

    // ---------------------------------------------------------------- Mining
    // minerios, gemas, blocos, mesa de lapidacao, pedras de forja, pedra filosofal, ferramentas e armaduras
    public static final Supplier<CreativeModeTab> MINING_TAB = CREATIVE_MODE_TAB.register("mining",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(MiningItems.MINING_TAB_ICON.get()))
                    .title(Component.translatable("creativetab.binosmining.mining"))
                    .displayItems((params, output) -> {
                        output.accept(MiningBlocks.RUBI_ORE);
                        output.accept(MiningBlocks.RUBI_DEEPSLATE_ORE);
                        output.accept(MiningBlocks.SAPPHIRE_ORE);
                        output.accept(MiningBlocks.SAPPHIRE_DEEPSLATE_ORE);
                        output.accept(MiningBlocks.JADE_ORE);
                        output.accept(MiningBlocks.JADE_DEEPSLATE_ORE);
                        output.accept(MiningBlocks.FORGE_ORE);
                        output.accept(MiningItems.RAW_DIAMOND);
                        output.accept(MiningItems.RAW_RUBI);
                        output.accept(MiningItems.RAW_SAPPHIRE);
                        output.accept(MiningItems.RAW_JADE);
                        output.accept(MiningItems.RUBI);
                        output.accept(MiningItems.SAPPHIRE);
                        output.accept(MiningItems.JADE);
                        output.accept(MiningBlocks.RUBI_BLOCK);
                        output.accept(MiningBlocks.SAPPHIRE_BLOCK);
                        output.accept(MiningBlocks.JADE_BLOCK);
                        output.accept(MiningBlocks.PHILOSOPHER_BLOCK);
                        output.accept(MiningBlocks.LAPIDARY_TABLE);
                        output.accept(MiningItems.FORGE_STONE_1);
                        output.accept(MiningItems.FORGE_STONE_2);
                        output.accept(MiningItems.FORGE_STONE_3);
                        output.accept(MiningItems.FORGE_STONE_4);
                        output.accept(MiningItems.FORGE_STONE_5);
                        output.accept(MiningItems.FORGE_STONE_6);
                        output.accept(MiningItems.FORGE_STONE_7);
                        output.accept(MiningItems.FORGE_STONE_8);
                        output.accept(MiningItems.FORGE_STONE_9);
                        output.accept(MiningItems.FORGE_STONE_10);
                        output.accept(MiningItems.PHILOSOPHER_FRAGMENT);
                        output.accept(MiningItems.PHILOSOPHER_STONE_INCOMPLETE);
                        output.accept(MiningItems.PHILOSOPHER_STONE_SMALL);
                        output.accept(MiningItems.PHILOSOPHER_STONE_LARGE);
                        output.accept(MiningItems.RUBI_SWORD);
                        output.accept(MiningItems.RUBI_PICKAXE);
                        output.accept(MiningItems.RUBI_SHOVEL);
                        output.accept(MiningItems.RUBI_AXE);
                        output.accept(MiningItems.RUBI_HOE);
                        output.accept(MiningItems.RUBI_HELMET);
                        output.accept(MiningItems.RUBI_CHESTPLATE);
                        output.accept(MiningItems.RUBI_LEGGINGS);
                        output.accept(MiningItems.RUBI_BOOTS);
                        output.accept(MiningItems.SAPPHIRE_SWORD);
                        output.accept(MiningItems.SAPPHIRE_PICKAXE);
                        output.accept(MiningItems.SAPPHIRE_SHOVEL);
                        output.accept(MiningItems.SAPPHIRE_AXE);
                        output.accept(MiningItems.SAPPHIRE_HOE);
                        output.accept(MiningItems.SAPPHIRE_HELMET);
                        output.accept(MiningItems.SAPPHIRE_CHESTPLATE);
                        output.accept(MiningItems.SAPPHIRE_LEGGINGS);
                        output.accept(MiningItems.SAPPHIRE_BOOTS);
                        output.accept(MiningItems.JADE_SWORD);
                        output.accept(MiningItems.JADE_PICKAXE);
                        output.accept(MiningItems.JADE_SHOVEL);
                        output.accept(MiningItems.JADE_AXE);
                        output.accept(MiningItems.JADE_HOE);
                        output.accept(MiningItems.JADE_HELMET);
                        output.accept(MiningItems.JADE_CHESTPLATE);
                        output.accept(MiningItems.JADE_LEGGINGS);
                        output.accept(MiningItems.JADE_BOOTS);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
