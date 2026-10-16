package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    // Nome (chave) do veio de rubi: binosvanillaplus:rubi_ore
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBI_ORE_KEY = registerKey("rubi_ore");

    // O datagen chama este metodo para criar o conteúdo
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Em quais blocos o minério pode nascer: pedra e afins / ardósia e afins
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceable = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        // Dentro de pedra vira rubi_ore, dentro de ardósia vira rubi_deepslate_ore
        List<OreConfiguration.TargetBlockState> rubiOres = List.of(
                OreConfiguration.target(stoneReplaceable, ModBlocks.RUBI_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceable, ModBlocks.RUBI_DEEPSLATE_ORE.get().defaultBlockState()));

        // TESTE: veio de tamanho 30, sem descartar blocos expostos ao ar
        register(context, RUBI_ORE_KEY, Feature.ORE, new OreConfiguration(rubiOres, 30, 0.0f));
    }

    // Cria a chave com o nome do seu mod
    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }

    // Metodo auxiliar para não repetir código a cada feature nova
    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}