package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.item.custom.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import java.util.List;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS  = DeferredRegister.createItems(BinosVanillaMod.MOD_ID);

    // ovos das faccoes
    public static final DeferredItem<Item> VILLAGER_GUARD_SPAWN_EGG = ITEMS.register("villager_guard_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.VILLAGER_GUARD, 0x6B4B2A, 0x5DA34F, new Item.Properties()));
    public static final DeferredItem<Item> VILLAGER_MAGE_SPAWN_EGG = ITEMS.register("villager_mage_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.VILLAGER_MAGE, 0x2E6B3F, 0xBCA341, new Item.Properties()));
    public static final DeferredItem<Item> VILLAGER_WITCH_SPAWN_EGG = ITEMS.register("villager_witch_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.VILLAGER_WITCH, 0x3A6B2A, 0x5A3D24, new Item.Properties()));
    public static final DeferredItem<Item> VILLAGER_RAVAGER_SPAWN_EGG = ITEMS.register("villager_ravager_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.VILLAGER_RAVAGER, 0x6B4A30, 0x2F7A3A, new Item.Properties()));
    public static final DeferredItem<Item> PILLAGER_SMUGGLER_SPAWN_EGG = ITEMS.register("pillager_smuggler_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.PILLAGER_SMUGGLER, 0x2A2C32, 0x7A2A2E, new Item.Properties()));
    // Pillager civil = villager do jogo marcado como pillager (assim ele vive, trabalha e se reproduz como villager)
    public static final DeferredItem<Item> PILLAGER_CIVILIAN_SPAWN_EGG = ITEMS.register("pillager_civilian_spawn_egg",
            () -> new com.bino.binosvanillaplus.item.CivilianSpawnEggItem(new Item.Properties()));

    // Essencia do Recomeco: devolve os pontos de atributo (nivel do personagem, regras de 23/11)
    public static final DeferredItem<Item> RESPEC_ESSENCE = ITEMS.register("respec_essence",
            () -> new RespecEssenceItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // fireResistant() = o item não queima se cair na lava (tire se quiser que queime)
    public static final DeferredItem<CyalumeStickItem> CYALUME_STICK = ITEMS.register("cyalume_stick",
            () -> new CyalumeStickItem(new Item.Properties().fireResistant()));

    // Corda (a cada item = 1 bloco de corda) e Gancho de Escalada
    public static final DeferredItem<RopeItem> ROPE = ITEMS.register("rope",
            () -> new RopeItem(new Item.Properties()));
    public static final DeferredItem<ClimbingHookItem> CLIMBING_HOOK = ITEMS.register("climbing_hook",
            () -> new ClimbingHookItem(new Item.Properties().stacksTo(1)));

    // Chave Dimensional (cria a Porta Dimensional)
    public static final DeferredItem<DimensionalKeyItem> DIMENSIONAL_KEY = ITEMS.register("dimensional_key",
            () -> new DimensionalKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Lanterna de luz focalizada e Detonador da dinamite
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.FlashlightItem> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new com.bino.binosvanillaplus.item.custom.FlashlightItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.DetonatorItem> DETONATOR = ITEMS.register("detonator",
            () -> new com.bino.binosvanillaplus.item.custom.DetonatorItem(new Item.Properties().stacksTo(1)));

    // Apito de explorador e Sonar de exploracao
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.ExplorerWhistleItem> EXPLORER_WHISTLE = ITEMS.register("explorer_whistle",
            () -> new com.bino.binosvanillaplus.item.custom.ExplorerWhistleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.ExplorerSonarItem> EXPLORER_SONAR = ITEMS.register("explorer_sonar",
            () -> new com.bino.binosvanillaplus.item.custom.ExplorerSonarItem(new Item.Properties().stacksTo(1)));

    // Camera, Diario de Exploracao e Foto revelada
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.CameraItem> CAMERA = ITEMS.register("camera",
            () -> new com.bino.binosvanillaplus.item.custom.CameraItem(new Item.Properties().stacksTo(1)));
    // Cave Goblin: ovo com textura propria (cores brancas = sem tingir) e o anel raro que ele carrega
    public static final DeferredItem<Item> CAVE_GOBLIN_SPAWN_EGG = ITEMS.register("cave_goblin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.CAVE_GOBLIN, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    // durabilidade = segundos de invisibilidade (1200 = 20 min); gasta 1 por segundo so enquanto a invisibilidade esta ativa
    public static final DeferredItem<Item> INVISIBLE_RING = ITEMS.register("invisible_ring",
            () -> new Item(new Item.Properties().durability(1200).rarity(Rarity.EPIC)));
    // Rato: ovo e carne (efeito parecido com a carne podre, mas sacia mais)
    public static final DeferredItem<Item> RAT_SPAWN_EGG = ITEMS.register("rat_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.RAT, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    // Ovo de Mimico: guardar num bau e fechar transforma o bau em mimico (nao invoca nada no chao)
    // Agua Fervida (sede): garrafa de agua fervida na fornalha, defumador ou fogueira, sem risco de nausea
    public static final DeferredItem<Item> BOILED_WATER = ITEMS.register("boiled_water",
            () -> new com.bino.binosvanillaplus.thirst.BoiledWaterItem(new Item.Properties().stacksTo(64)));
    // Frascos do Bino (ver thirst/Vessels): vazio + cheio de cada tamanho (o tamanho 2 e' a garrafa do jogo)
    public static final DeferredItem<Item> VIAL = ITEMS.register("vial",
            () -> new com.bino.binosvanillaplus.thirst.VesselItem(1, () -> ModItems.FILLED_VIAL.get(), new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FILLED_VIAL = ITEMS.register("filled_vial",
            () -> new com.bino.binosvanillaplus.thirst.FilledVesselItem(1, 1, 16, () -> ModItems.VIAL.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FLASK = ITEMS.register("flask",
            () -> new com.bino.binosvanillaplus.thirst.VesselItem(3, () -> ModItems.FILLED_FLASK.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FILLED_FLASK = ITEMS.register("filled_flask",
            () -> new com.bino.binosvanillaplus.thirst.FilledVesselItem(3, 2, 32, () -> ModItems.FLASK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> LARGE_FLASK = ITEMS.register("large_flask",
            () -> new com.bino.binosvanillaplus.thirst.VesselItem(4, () -> ModItems.FILLED_LARGE_FLASK.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FILLED_LARGE_FLASK = ITEMS.register("filled_large_flask",
            () -> new com.bino.binosvanillaplus.thirst.FilledVesselItem(4, 3, 36, () -> ModItems.LARGE_FLASK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> JUG = ITEMS.register("jug",
            () -> new com.bino.binosvanillaplus.thirst.VesselItem(5, () -> ModItems.FILLED_JUG.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FILLED_JUG = ITEMS.register("filled_jug",
            () -> new com.bino.binosvanillaplus.thirst.FilledVesselItem(5, 4, 40, () -> ModItems.JUG.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MIMIC_SPAWN_EGG = ITEMS.register("mimic_spawn_egg",
            () -> new com.bino.binosvanillaplus.mimic.MimicEggItem(new Item.Properties()));
    public static final DeferredItem<Item> DIRE_RAT_SPAWN_EGG = ITEMS.register("dire_rat_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.bino.binosvanillaplus.entity.ModEntities.DIRE_RAT, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Item> RAT_MEAT = ITEMS.register("rat_meat",
            () -> new com.bino.binosvanillaplus.item.custom.ImmersiveFoodItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.4f)   // carne podre: 4 e 0,1
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 600, 0), 0.8f)
                    .build()), 48));   // 48 ticks = duracao da animacao eat_meat
    // pedaco de queijo (sai do queijo colocado no chao); o queijo inteiro e o item do bloco ModBlocks.CHEESE
    public static final DeferredItem<Item> COOKED_RAT_MEAT = ITEMS.register("cooked_rat_meat",
            () -> new com.bino.binosvanillaplus.item.custom.ImmersiveFoodItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build()), 48));
    public static final DeferredItem<Item> CHEESE_SLICE = ITEMS.register("cheese_slice",
            () -> new com.bino.binosvanillaplus.item.custom.ImmersiveFoodItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.6f).build()), 40));   // 40 = eat_cheese
    public static final java.util.function.Supplier<Item> CHEESE = () -> com.bino.binosvanillaplus.block.ModBlocks.CHEESE.get().asItem();
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.ExplorationDiaryItem> EXPLORATION_DIARY = ITEMS.register("exploration_diary",
            () -> new com.bino.binosvanillaplus.item.custom.ExplorationDiaryItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.bino.binosvanillaplus.item.custom.PhotoItem> PHOTO = ITEMS.register("photo",
            () -> new com.bino.binosvanillaplus.item.custom.PhotoItem(new Item.Properties().stacksTo(1)));


    // Item do tumulo: so existe para servir de icone no mapa (nao aparece no criativo, nao se crafta)
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GRAVE = ITEMS.register("grave",
            () -> new net.minecraft.world.item.BlockItem(com.bino.binosvanillaplus.block.ModBlocks.GRAVE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
