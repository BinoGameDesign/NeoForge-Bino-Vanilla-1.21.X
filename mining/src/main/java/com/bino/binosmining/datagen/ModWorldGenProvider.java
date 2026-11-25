package com.bino.binosmining.datagen;

import com.bino.binosmining.BinosMining;
import com.bino.binosmining.worldgen.ModBiomeModifiers;
import com.bino.binosmining.worldgen.ModConfiguredFeatures;
import com.bino.binosmining.worldgen.ModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    // Lista de tudo que o datagen deve gerar de geração de mundo
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        // Set.of(MOD_ID) limita a geração às entradas do seu mod
        super(output, registries, BUILDER, Set.of(BinosMining.MOD_ID));
    }
}