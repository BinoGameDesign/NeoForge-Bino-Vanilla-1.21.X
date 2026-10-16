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
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    // Tamanho máximo do veio. Diamante vanilla = 4. Para testar, suba para 30.
    private static final int VEIN_SIZE = 4;

    // Nome (chave) do veio: binosvanillaplus:rubi_ore
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBI_ORE_KEY = registerKey("rubi_ore");

    // O datagen chama este método para criar o conteúdo
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Em quais blocos o minério pode nascer: pedra e afins / ardósia e afins
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceable = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        // Dentro de pedra vira rubi_ore, dentro de ardósia vira rubi_deepslate_ore
        List<OreConfiguration.TargetBlockState> rubiOres = List.of(
                OreConfiguration.target(stoneReplaceable, ModBlocks.RUBI_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceable, ModBlocks.RUBI_DEEPSLATE_ORE.get().defaultBlockState()));

        // 0.5f = chance de descartar blocos que tocam o ar (igual ao diamante)
        context.register(RUBI_ORE_KEY,
                new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(rubiOres, VEIN_SIZE, 0f)));
    }

    // Cria a chave com o namespace do seu mod
    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }
}