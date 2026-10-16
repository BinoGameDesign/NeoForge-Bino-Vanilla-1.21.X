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
                    }).build());

    public static final Supplier<CreativeModeTab> MINING_BLOCK_TAB = CREATIVE_MODE_TAB.register("mining_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.RUBI_ORE))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mining_items_tab"))
                    .title(Component.translatable("creativetab.binosvanillaplus.mining_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.RUBI_ORE);
                        output.accept(ModBlocks.RUBI_DEEPSLATE_ORE);
                        output.accept(ModBlocks.RUBI_BLOCK);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}