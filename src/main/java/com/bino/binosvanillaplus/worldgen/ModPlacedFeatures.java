package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> RUBI_ORE_PLACED_KEY = registerKey("rubi_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        // Busca o veio que criamos no arquivo anterior
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, RUBI_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.RUBI_ORE_KEY),
                List.of(
                        CountPlacement.of(7),          // 7 tentativas de veio por chunk (igual ao diamante)
                        InSquarePlacement.spread(),    // espalha as tentativas em posições X/Z aleatórias
                        // Altura: de Y=-144 a Y=16, mais comum no meio da faixa (o "triângulo").
                        // aboveBottom(-80) significa "80 blocos abaixo do fundo do mundo (Y=-64)", ou seja, Y=-144
                        HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)),
                        BiomeFilter.biome()            // só gera nos biomas liberados pelo BiomeModifier
                ));
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context,
                                 ResourceKey<PlacedFeature> key,
                                 Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}