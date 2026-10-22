package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
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
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAPPHIRE_ORE_KEY = registerKey("sapphire_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> JADE_ORE_KEY = registerKey("jade_ore");

    // O datagen chama este método para criar o conteúdo
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Um veio para cada gema: o mesmo método cria os três
        registerOre(context, RUBI_ORE_KEY, ModBlocks.RUBI_ORE.get(), ModBlocks.RUBI_DEEPSLATE_ORE.get());
        registerOre(context, SAPPHIRE_ORE_KEY, ModBlocks.SAPPHIRE_ORE.get(), ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        registerOre(context, JADE_ORE_KEY, ModBlocks.JADE_ORE.get(), ModBlocks.JADE_DEEPSLATE_ORE.get());
    }

    // Cria um veio: dentro de pedra vira o minério normal, dentro de ardósia vira o minério de ardósia
    private static void registerOre(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                    ResourceKey<ConfiguredFeature<?, ?>> key, Block ore, Block deepslateOre) {
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceable = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<OreConfiguration.TargetBlockState> targets = List.of(
                OreConfiguration.target(stoneReplaceable, ore.defaultBlockState()),
                OreConfiguration.target(deepslateReplaceable, deepslateOre.defaultBlockState()));

        // 0f = não descarta blocos que tocam o ar
        context.register(key, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, VEIN_SIZE, 0f)));
    }

    // Cria a chave com o namespace do seu mod
    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name));
    }
}