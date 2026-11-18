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

// Abas do modo criativo, uma por mod do futuro conjunto (cada mod funciona sozinho e melhora quando usado com os outros):
//   mining_*      -> Mining       (gemas, equipamentos, forja, lapidario)
//   exploration_* -> Exploration  (corda, gancho, lanterna, dinamite, camera, sonar...)
//   adventure_*   -> Adventure    (mapa, Allay, tumulo, pings, dimensao portatil, fogueiras, niveis de mobs)
//   faction_*     -> Faction      (ovos de spawn das faccoes)
// New Menus (inventario novo e receitas) nao tem itens, entao nao tem aba.
// Ficam de fora de proposito: foto, tumulo, luz da lanterna, parede e porta da dimensao.
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BinosVanillaMod.MOD_ID);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, path);
    }

    // ---------------------------------------------------------------- Mining
    // minerios, gemas, blocos, mesa de lapidacao, pedras de forja, pedra filosofal, ferramentas e armaduras
    public static final Supplier<CreativeModeTab> MINING_TAB = CREATIVE_MODE_TAB.register("mining",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MINING_TAB_ICON.get()))
                    .title(Component.translatable("creativetab.binosvanillaplus.mining"))
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

    // ---------------------------------------------------------------- Exploration
    public static final Supplier<CreativeModeTab> EXPLORATION_TAB = CREATIVE_MODE_TAB.register("exploration",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CYALUME_STICK.get()))
                    .withTabsBefore(id("mining"))
                    .title(Component.translatable("creativetab.binosvanillaplus.exploration"))
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
                        output.accept(ModItems.INVISIBLE_RING);
                        output.accept(ModItems.CAVE_GOBLIN_SPAWN_EGG);
                        output.accept(ModItems.RAT_MEAT);
                        output.accept(ModItems.COOKED_RAT_MEAT);
                        output.accept(com.bino.binosvanillaplus.block.ModBlocks.CHEESE);
                        output.accept(ModItems.CHEESE_SLICE);
                        output.accept(com.bino.binosvanillaplus.block.ModBlocks.RAT_TRAP);
                        output.accept(com.bino.binosvanillaplus.block.ModBlocks.CAGE);
                        output.accept(ModItems.RAT_SPAWN_EGG);
                        output.accept(ModBlocks.DYNAMITE);
                        output.accept(ModBlocks.CAMERA_TRIPOD);
                        output.accept(ModBlocks.FOSSIL_SKULL);
                        output.accept(ModBlocks.FOSSIL_VERTEBRA);
                        output.accept(ModBlocks.FOSSIL_RIB);
                        output.accept(ModBlocks.FOSSIL_LIMB);
                    }).build());

    // ---------------------------------------------------------------- Adventure
    public static final Supplier<CreativeModeTab> ADVENTURE_TAB = CREATIVE_MODE_TAB.register("adventure",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.DIMENSIONAL_KEY.get()))
                    .withTabsBefore(id("exploration"))
                    .title(Component.translatable("creativetab.binosvanillaplus.adventure"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.DIMENSIONAL_KEY);
                    }).build());

    // ---------------------------------------------------------------- Faction
    public static final Supplier<CreativeModeTab> FACTION_TAB = CREATIVE_MODE_TAB.register("faction",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.VILLAGER_GUARD_SPAWN_EGG.get()))
                    .withTabsBefore(id("adventure"))
                    .title(Component.translatable("creativetab.binosvanillaplus.faction"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.VILLAGER_GUARD_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_MAGE_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_WITCH_SPAWN_EGG);
                        output.accept(ModItems.VILLAGER_RAVAGER_SPAWN_EGG);
                        output.accept(ModItems.PILLAGER_SMUGGLER_SPAWN_EGG);
                        output.accept(ModItems.PILLAGER_CIVILIAN_SPAWN_EGG);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
