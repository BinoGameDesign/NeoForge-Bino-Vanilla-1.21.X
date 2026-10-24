package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.custom.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import java.util.List;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS  = DeferredRegister.createItems(BinosVanillaMod.MOD_ID);

    public static final DeferredItem<Item> RUBI = ITEMS.register("rubi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<SwordItem> RUBI_SWORD = ITEMS.register("rubi_sword",
            () -> new SwordItem(ModToolTiers.RUBI, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.RUBI,3,-1.92f))));
    public static final DeferredItem<PickaxeItem> RUBI_PICKAXE = ITEMS.register("rubi_pickaxe",
            () -> new RubiPickaxeItem(ModToolTiers.RUBI, new Item.Properties()   // antes: new PickaxeItem
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.RUBI, 1, -2.8f))));

    public static final DeferredItem<ShovelItem> RUBI_SHOVEL = ITEMS.register("rubi_shovel",
            () -> new RubiShovelItem(ModToolTiers.RUBI, new Item.Properties()    // antes: new ShovelItem
                    .attributes(ShovelItem.createAttributes(ModToolTiers.RUBI, 1.5f, -3f))));

    public static final DeferredItem<AxeItem> RUBI_AXE = ITEMS.register("rubi_axe",
            () -> new RubiAxeItem(ModToolTiers.RUBI, new Item.Properties()       // antes: new AxeItem
                    .attributes(AxeItem.createAttributes(ModToolTiers.RUBI, 5, -3.0f))));

    public static final DeferredItem<HoeItem> RUBI_HOE = ITEMS.register("rubi_hoe",
            () -> new RubiHoeItem(ModToolTiers.RUBI, new Item.Properties()       // antes: new HoeItem
                    .attributes(HoeItem.createAttributes(ModToolTiers.RUBI, -3, 0f))));

    // ===== SAPPHIRE =====  (ferramentas comuns, com os mesmos números das de diamante vanilla)
    public static final DeferredItem<Item> SAPPHIRE = ITEMS.register("sapphire",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<SwordItem> SAPPHIRE_SWORD = ITEMS.register("sapphire_sword",
            () -> new SwordItem(ModToolTiers.SAPPHIRE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.SAPPHIRE, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> SAPPHIRE_PICKAXE = ITEMS.register("sapphire_pickaxe",
            () -> new PickaxeItem(ModToolTiers.SAPPHIRE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.SAPPHIRE, 1, -2.8f))));
    public static final DeferredItem<ShovelItem> SAPPHIRE_SHOVEL = ITEMS.register("sapphire_shovel",
            () -> new ShovelItem(ModToolTiers.SAPPHIRE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.SAPPHIRE, 1.5f, -3.0f))));
    public static final DeferredItem<AxeItem> SAPPHIRE_AXE = ITEMS.register("sapphire_axe",
            () -> new AxeItem(ModToolTiers.SAPPHIRE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.SAPPHIRE, 5, -3.0f))));
    public static final DeferredItem<HoeItem> SAPPHIRE_HOE = ITEMS.register("sapphire_hoe",
            () -> new HoeItem(ModToolTiers.SAPPHIRE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTiers.SAPPHIRE, -3, 0.0f))));

    // ===== JADE =====  (ferramentas comuns, com os mesmos números das de diamante vanilla)
    public static final DeferredItem<Item> JADE = ITEMS.register("jade",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<SwordItem> JADE_SWORD = ITEMS.register("jade_sword",
            () -> new SwordItem(ModToolTiers.JADE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.JADE, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> JADE_PICKAXE = ITEMS.register("jade_pickaxe",
            () -> new PickaxeItem(ModToolTiers.JADE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.JADE, 1, -2.8f))));
    public static final DeferredItem<ShovelItem> JADE_SHOVEL = ITEMS.register("jade_shovel",
            () -> new ShovelItem(ModToolTiers.JADE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.JADE, 1.5f, -3.0f))));
    public static final DeferredItem<AxeItem> JADE_AXE = ITEMS.register("jade_axe",
            () -> new AxeItem(ModToolTiers.JADE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.JADE, 5, -3.0f))));
    public static final DeferredItem<HoeItem> JADE_HOE = ITEMS.register("jade_hoe",
            () -> new HoeItem(ModToolTiers.JADE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTiers.JADE, -3, 0.0f))));

    // ===== Armadura de Rubi =====
    public static final DeferredItem<ArmorItem> RUBI_HELMET = ITEMS.register("rubi_helmet",
            () -> new ArmorItem(ModArmorMaterials.RUBI, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> RUBI_CHESTPLATE = ITEMS.register("rubi_chestplate",
            () -> new ArmorItem(ModArmorMaterials.RUBI, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> RUBI_LEGGINGS = ITEMS.register("rubi_leggings",
            () -> new ArmorItem(ModArmorMaterials.RUBI, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> RUBI_BOOTS = ITEMS.register("rubi_boots",
            () -> new ArmorItem(ModArmorMaterials.RUBI, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(33))));   // 33 = multiplicador do diamante

    // ===== Armadura de Safira =====
    public static final DeferredItem<ArmorItem> SAPPHIRE_HELMET = ITEMS.register("sapphire_helmet",
            () -> new ArmorItem(ModArmorMaterials.SAPPHIRE, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> SAPPHIRE_CHESTPLATE = ITEMS.register("sapphire_chestplate",
            () -> new ArmorItem(ModArmorMaterials.SAPPHIRE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> SAPPHIRE_LEGGINGS = ITEMS.register("sapphire_leggings",
            () -> new ArmorItem(ModArmorMaterials.SAPPHIRE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> SAPPHIRE_BOOTS = ITEMS.register("sapphire_boots",
            () -> new ArmorItem(ModArmorMaterials.SAPPHIRE, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(33))));   // 33 = multiplicador do diamante

    // ===== Armadura de Jade =====
    public static final DeferredItem<ArmorItem> JADE_HELMET = ITEMS.register("jade_helmet",
            () -> new ArmorItem(ModArmorMaterials.JADE, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> JADE_CHESTPLATE = ITEMS.register("jade_chestplate",
            () -> new ArmorItem(ModArmorMaterials.JADE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> JADE_LEGGINGS = ITEMS.register("jade_leggings",
            () -> new ArmorItem(ModArmorMaterials.JADE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));   // 33 = multiplicador do diamante
    public static final DeferredItem<ArmorItem> JADE_BOOTS = ITEMS.register("jade_boots",
            () -> new ArmorItem(ModArmorMaterials.JADE, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(33))));   // 33 = multiplicador do diamante

    // ===== FORJA =====
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_1 = ITEMS.register("forge_stone_1",
            () -> new ForgeStoneItem(1, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_2 = ITEMS.register("forge_stone_2",
            () -> new ForgeStoneItem(2, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_3 = ITEMS.register("forge_stone_3",
            () -> new ForgeStoneItem(3, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_4 = ITEMS.register("forge_stone_4",
            () -> new ForgeStoneItem(4, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_5 = ITEMS.register("forge_stone_5",
            () -> new ForgeStoneItem(5, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_6 = ITEMS.register("forge_stone_6",
            () -> new ForgeStoneItem(6, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_7 = ITEMS.register("forge_stone_7",
            () -> new ForgeStoneItem(7, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_8 = ITEMS.register("forge_stone_8",
            () -> new ForgeStoneItem(8, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_9 = ITEMS.register("forge_stone_9",
            () -> new ForgeStoneItem(9, new Item.Properties()));
    public static final DeferredItem<ForgeStoneItem> FORGE_STONE_10 = ITEMS.register("forge_stone_10",
            () -> new ForgeStoneItem(10, new Item.Properties()));
    // Lista em ordem: FORGE_STONES.get(0) = +1 ... get(9) = +10
    public static final List<DeferredItem<ForgeStoneItem>> FORGE_STONES = List.of(
            FORGE_STONE_1, FORGE_STONE_2, FORGE_STONE_3, FORGE_STONE_4, FORGE_STONE_5, FORGE_STONE_6, FORGE_STONE_7, FORGE_STONE_8, FORGE_STONE_9, FORGE_STONE_10);

    public static final DeferredItem<Item> PHILOSOPHER_FRAGMENT = ITEMS.register("philosopher_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PHILOSOPHER_STONE_INCOMPLETE = ITEMS.register("philosopher_stone_incomplete",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PHILOSOPHER_STONE_SMALL = ITEMS.register("philosopher_stone_small",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PHILOSOPHER_STONE_LARGE = ITEMS.register("philosopher_stone_large",
            () -> new Item(new Item.Properties()));

    // ===== Minérios brutos (soltos pelos minérios; viram gema na Mesa de Lapidação) =====
    public static final DeferredItem<Item> RAW_DIAMOND = ITEMS.register("raw_diamond",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_RUBI = ITEMS.register("raw_rubi",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_SAPPHIRE = ITEMS.register("raw_sapphire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_JADE = ITEMS.register("raw_jade",
            () -> new Item(new Item.Properties()));

    // fireResistant() = o item não queima se cair na lava (tire se quiser que queime)
    public static final DeferredItem<CyalumeStickItem> CYALUME_STICK = ITEMS.register("cyalume_stick",
            () -> new CyalumeStickItem(new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
