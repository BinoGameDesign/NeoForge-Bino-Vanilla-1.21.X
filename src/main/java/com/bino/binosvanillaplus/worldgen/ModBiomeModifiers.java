package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_RUBI_ORE = registerKey("add_rubi_ore");
    public static final ResourceKey<BiomeModifier> ADD_SAPPHIRE_ORE = registerKey("add_sapphire_ore");
    public static final ResourceKey<BiomeModifier> ADD_JADE_ORE = registerKey("add_jade_ore");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        // "Adicione esta feature em todos os biomas do Overworld, na etapa de minérios subterrâneos"
        addOre(context, biomes, placedFeatures, ADD_RUBI_ORE, ModPlacedFeatures.RUBI_ORE_PLACED_KEY);
        addOre(context, biomes, placedFeatures, ADD_SAPPHIRE_ORE, ModPlacedFeatures.SAPPHIRE_ORE_PLACED_KEY);
        addOre(context, biomes, placedFeatures, ADD_JADE_ORE, ModPlacedFeatures.JADE_ORE_PLACED_KEY);
    }

    private static void addOre(BootstrapContext<BiomeModifier> context, HolderGetter<Biome> biomes,
                               HolderGetter<PlacedFeature> placedFeatures,
                               ResourceKey<BiomeModifier> key, ResourceKey<PlacedFeature> placedKey) {
        context.register(key, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(placedKey)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    public static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }
}