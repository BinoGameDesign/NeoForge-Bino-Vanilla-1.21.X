package com.bino.binosvanillaplus.menu;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Tipos de menu (janelas) do mod
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BinosVanillaMod.MOD_ID);

    public static final Supplier<MenuType<LapidaryMenu>> LAPIDARY = MENUS.register("lapidary_table",
            () -> new MenuType<>(LapidaryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<SpiritMenu>> SPIRIT = MENUS.register("spirit",
            () -> new MenuType<>(SpiritMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
