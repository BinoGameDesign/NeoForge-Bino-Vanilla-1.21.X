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

    // ---------------------------------------------------------------- Exploration
    public static final Supplier<CreativeModeTab> EXPLORATION_TAB = CREATIVE_MODE_TAB.register("exploration",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CYALUME_STICK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath("binosmining", "mining"))   // logo depois da aba do Mining
                    .title(Component.translatable("creativetab.binosvanillaplus.exploration"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CYALUME_STICK);
                        output.accept(ModItems.RESPEC_ESSENCE);
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
                        output.accept(ModItems.DIRE_RAT_SPAWN_EGG);
                        output.accept(ModItems.MIMIC_SPAWN_EGG);
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
                        // sede: garrafas e frascos
                        output.accept(net.minecraft.world.item.Items.GLASS_BOTTLE);
                        output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,
                                net.minecraft.world.item.alchemy.Potions.WATER));
                        output.accept(ModItems.BOILED_WATER);
                        for (var v : java.util.List.of(ModItems.VIAL, ModItems.FLASK, ModItems.LARGE_FLASK, ModItems.JUG)) output.accept(v);
                        for (var v : java.util.List.of(ModItems.FILLED_VIAL, ModItems.FILLED_FLASK, ModItems.FILLED_LARGE_FLASK, ModItems.FILLED_JUG)) {
                            var f = (com.bino.binosvanillaplus.thirst.FilledVesselItem) v.get();
                            output.accept(f.getDefaultInstance());
                            output.accept(com.bino.binosvanillaplus.thirst.Vessels.filled(f,
                                    new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER), true));
                        }
                        // temperatura
                        output.accept(ModItems.WOOL_LINING);
                        output.accept(ModItems.LIGHT_LINING);
                        output.accept(ModItems.HOT_TEA);
                        output.accept(ModItems.REFRESHMENT);
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
