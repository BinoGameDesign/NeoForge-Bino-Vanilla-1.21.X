package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

public class ModPlacedFeatures {

    // Tentativas de veio por chunk. Diamante vanilla = 7. Para testar, suba para 100.
    private static final int VEINS_PER_CHUNK = 7;

    public static final ResourceKey<PlacedFeature> RUBI_ORE_PLACED_KEY = registerKey("rubi_ore_placed");
    public static final ResourceKey<PlacedFeature> SAPPHIRE_ORE_PLACED_KEY = registerKey("sapphire_ore_placed");
    public static final ResourceKey<PlacedFeature> JADE_ORE_PLACED_KEY = registerKey("jade_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // Você pode dar uma quantidade diferente de veios para cada gema (último número)
        registerOre(context, configuredFeatures, RUBI_ORE_PLACED_KEY, ModConfiguredFeatures.RUBI_ORE_KEY, VEINS_PER_CHUNK);
        registerOre(context, configuredFeatures, SAPPHIRE_ORE_PLACED_KEY, ModConfiguredFeatures.SAPPHIRE_ORE_KEY, VEINS_PER_CHUNK);
        registerOre(context, configuredFeatures, JADE_ORE_PLACED_KEY, ModConfiguredFeatures.JADE_ORE_KEY, VEINS_PER_CHUNK);
    }

    private static void registerOre(BootstrapContext<PlacedFeature> context,
                                    HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures,
                                    ResourceKey<PlacedFeature> placedKey,
                                    ResourceKey<ConfiguredFeature<?, ?>> configuredKey, int veinsPerChunk) {
        context.register(placedKey, new PlacedFeature(
                configuredFeatures.getOrThrow(configuredKey),
                List.of(
                        CountPlacement.of(veinsPerChunk),     // quantas tentativas por chunk
                        InSquarePlacement.spread(),           // espalha em posições X/Z aleatórias
                        // Altura igual à do diamante: de Y=-144 a Y=16, mais comum perto de Y=-64
                        HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-64), VerticalAnchor.aboveBottom(100)),
                        BiomeFilter.biome()                   // só nos biomas liberados pelo BiomeModifier
                )));
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }
}