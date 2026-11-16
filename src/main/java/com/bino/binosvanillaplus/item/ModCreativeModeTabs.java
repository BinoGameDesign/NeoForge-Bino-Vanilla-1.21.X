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

// Abas do modo criativo, uma (ou duas) por mod do futuro conjunto. Ids com o prefixo do mod:
//   gems_*    -> Gemas & Forja        explore_*  -> Exploracao
//   world_*   -> Mundo Vivo           pocket_*   -> Dimensao Portatil
// Todas aparecem em sequencia, nesta ordem. Itens que nao devem aparecer no criativo (foto, tumulo, luz da lanterna,
// parede e porta da dimensao) ficam de fora de proposito.
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BinosVanillaMod.MOD_ID);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, path);
    }

    // ---------------------------------------------------------------- Gemas & Forja
    // materiais, minerios, blocos, mesa de lapidacao, pedras de forja e pedra filosofal
    public static final Supplier<CreativeModeTab> GEMS_MATERIALS_TAB = CREATIVE_MODE_TAB.register("gems_materials",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.RUBI.get()))
                    .title(Component.translatable("creativetab.binosvanillaplus.gems_materials"))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.RUBI_ORE);
                        output.accept(ModBlocks.RUBI_DEEPSLATE_ORE);
                        output.accept(ModBlocks.SAPPHIRE_ORE);
                        output.accept(ModBlocks.SAPPHIRE_DEEPSLATE_ORE);
                        output.accept(ModBlocks.JADE_ORE);
                        output.accept(ModBlocks.JADE_DEEPSLATE_ORE);
                        output.accept(ModBlocks.FORGE_ORE);
                        output.accept(ModItems.RAW_DIAMOND);
                        output.accept(ModItems.RAW_RUBI);
                        output.accept(ModItems.RAW_SAPPHIRE);
                        output.accept(ModItems.RAW_JADE);
                        output.accept(ModItems.RUBI);
                        output.accept(ModItems.SAPPHIRE);
                        output.accept(ModItems.JADE);
                        output.accept(ModBlocks.RUBI_BLOCK);
                        output.accept(ModBlocks.SAPPHIRE_BLOCK);
                        output.accept(ModBlocks.JADE_BLOCK);
                        output.accept(ModBlocks.PHILOSOPHER_BLOCK);
                        output.accept(ModBlocks.LAPIDARY_TABLE);
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
                    }).build());

    // ferramentas e armaduras de cada gema (rubi, safira, jade)
    public static final Supplier<CreativeModeTab> GEMS_GEAR_TAB = CREATIVE_MODE_TAB.register("gems_gear",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.RUBI_SWORD.get()))
                    .withTabsBefore(id("gems_materials"))
                    .title(Component.translatable("creativetab.binosvanillaplus.gems_gear"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.RUBI_SWORD);
                        output.accept(ModItems.RUBI_PICKAXE);
                        output.accept(ModItems.RUBI_SHOVEL);
                        output.accept(ModItems.RUBI_AXE);
                        output.accept(ModItems.RUBI_HOE);
                        output.accept(ModItems.RUBI_HELMET);
                        output.accept(ModItems.RUBI_CHESTPLATE);
                        output.accept(ModItems.RUBI_LEGGINGS);
                        output.accept(ModItems.RUBI_BOOTS);
                        output.accept(ModItems.SAPPHIRE_SWORD);
                        output.accept(ModItems.SAPPHIRE_PICKAXE);
                        output.accept(ModItems.SAPPHIRE_SHOVEL);
                        output.accept(ModItems.SAPPHIRE_AXE);
                        output.accept(ModItems.SAPPHIRE_HOE);
                        output.accept(ModItems.SAPPHIRE_HELMET);
                        output.accept(ModItems.SAPPHIRE_CHESTPLATE);
                        output.accept(ModItems.SAPPHIRE_LEGGINGS);
                        output.accept(ModItems.SAPPHIRE_BOOTS);
                        output.accept(ModItems.JADE_SWORD);
                        output.accept(ModItems.JADE_PICKAXE);
                        output.accept(ModItems.JADE_SHOVEL);
                        output.accept(ModItems.JADE_AXE);
                        output.accept(ModItems.JADE_HOE);
                        output.accept(ModItems.JADE_HELMET);
                        output.accept(ModItems.JADE_CHESTPLATE);
                        output.accept(ModItems.JADE_LEGGINGS);
                        output.accept(ModItems.JADE_BOOTS);
                    }).build());

    // ---------------------------------------------------------------- Exploracao
    public static final Supplier<CreativeModeTab> EXPLORE_TAB = CREATIVE_MODE_TAB.register("explore_tools",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CYALUME_STICK.get()))
                    .withTabsBefore(id("gems_gear"))
                    .title(Component.translatable("creativetab.binosvanillaplus.explore_tools"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CYALUME_STICK);
                        output.accept(ModItems.FLASHLIGHT);
                        output.accept(ModItems.ROPE);
                        output.accept(ModItems.CLIMBING_HOOK);
                        output.accept(ModItems.DETONATOR);
                        output.accept(ModItems.EXPLORER_WHISTLE);
                        output.accept(ModItems.EXPLORER_SONAR);
                        output.accept(ModItems.CAMERA);
                        output.accept(ModItems.EXPLORATION_DIARY);
                        output.accept(ModBlocks.DYNAMITE);
                        output.accept(ModBlocks.CAMERA_TRIPOD);
                        output.accept(ModBlocks.FOSSIL_SKULL);
                        output.accept(ModBlocks.FOSSIL_VERTEBRA);
                        output.accept(ModBlocks.FOSSIL_RIB);
                        output.accept(ModBlocks.FOSSIL_LIMB);
                    }).build());

    // ---------------------------------------------------------------- Mundo Vivo
    public static final Supplier<CreativeModeTab> WORLD_TAB = CREATIVE_MODE_TAB.register("world_spawn_eggs",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.VILLAGER_GUARD_SPAWN_EGG.get()))
                    .withTabsBefore(id("explore_tools"))
                    .title(Component.translatable("creativetab.binosvanillaplus.world_spawn_eggs"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.VILLAGER_GUARD_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_MAGE_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_WITCH_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_RAVAGER_SPAWN_EGG);
                        output.accept(ModItems.PILLAGER_SMUGGLER_SPAWN_EGG);
                        output.accept(ModItems.PILLAGER_CIVILIAN_SPAWN_EGG);
                    }).build());

    // ---------------------------------------------------------------- Dimensao Portatil
    public static final Supplier<CreativeModeTab> POCKET_TAB = CREATIVE_MODE_TAB.register("pocket_dimension",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.DIMENSIONAL_KEY.get()))
                    .withTabsBefore(id("world_spawn_eggs"))
                    .title(Component.translatable("creativetab.binosvanillaplus.pocket_dimension"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.DIMENSIONAL_KEY);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
