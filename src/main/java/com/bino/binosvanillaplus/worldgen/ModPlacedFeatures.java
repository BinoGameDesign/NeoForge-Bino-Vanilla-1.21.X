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

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        // Busca o veio criado no arquivo anterior
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(RUBI_ORE_PLACED_KEY, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.RUBI_ORE_KEY),
                List.of(
                        CountPlacement.of(VEINS_PER_CHUNK),   // quantas tentativas por chunk
                        InSquarePlacement.spread(),           // espalha em posições X/Z aleatórias
                        // Altura igual à do diamante: de Y=-144 a Y=16, mais comum perto de Y=-64.
                        // aboveBottom(-80) = 80 blocos abaixo do fundo do mundo (Y=-64), ou seja, Y=-144.
                        HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-64), VerticalAnchor.aboveBottom(100)),
                        BiomeFilter.biome()                   // só nos biomas liberados pelo BiomeModifier
                )));
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }
}