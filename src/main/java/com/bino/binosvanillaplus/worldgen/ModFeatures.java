package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Geracao no mundo: fosseis (data/binosvanillaplus/worldgen/... e neoforge/biome_modifier/...)
public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, BinosVanillaMod.MOD_ID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> FOSSIL_BURIED =
            FEATURES.register("fossil_buried", () -> new FossilFeature(NoneFeatureConfiguration.CODEC, false));
    public static final Supplier<Feature<NoneFeatureConfiguration>> FOSSIL_EXPOSED =
            FEATURES.register("fossil_exposed", () -> new FossilFeature(NoneFeatureConfiguration.CODEC, true));

    public static void register(IEventBus bus) { FEATURES.register(bus); }
}
