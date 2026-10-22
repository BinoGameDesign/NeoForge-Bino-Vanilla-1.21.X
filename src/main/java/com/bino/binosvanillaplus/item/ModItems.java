package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.custom.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
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

    // fireResistant() = o item não queima se cair na lava (tire se quiser que queime)
    public static final DeferredItem<CyalumeStickItem> CYALUME_STICK = ITEMS.register("cyalume_stick",
            () -> new CyalumeStickItem(new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
