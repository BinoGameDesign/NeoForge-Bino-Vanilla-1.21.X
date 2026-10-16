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

    // fireResistant() = o item não queima se cair na lava (tire se quiser que queime)
    public static final DeferredItem<CyalumeStickItem> CYALUME_STICK = ITEMS.register("cyalume_stick",
            () -> new CyalumeStickItem(new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
