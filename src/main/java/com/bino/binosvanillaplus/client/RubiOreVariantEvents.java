package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.VariantOreBlock;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

// Troca o modelo dos minerios por um que escolhe entre 8 modelos pela posicao (ver RubiOreVariantModel).
//  - Nossos minerios (rubi, safira, jade): os 8 modelos vem do blockstate (propriedade "variant").
//  - Minerios de diamante (bloco do jogo base, sem a propriedade): os 8 modelos sao carregados como modelos extras.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class RubiOreVariantEvents {

    private static final List<String> VANILLA_ORES = List.of("diamond_ore", "deepslate_diamond_ore");

    // Quantas variantes ja tem as fases de mineracao (b, c, d). Aumentar quando chegarem os modelos das outras.
    public static final int STAGED_VARIANTS = 8;
    private static final String[] STAGE_NAMES = {"", "b", "c", "d"};

    private static ModelResourceLocation extra(String ore, int i) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/" + ore + "_v" + i));
    }

    private static ModelResourceLocation stage(String ore, int i, int st) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/" + ore + "_v" + i + "_" + STAGE_NAMES[st]));
    }

    private static String modelName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (String ore : VANILLA_ORES) for (int i = 1; i <= 8; i++) event.register(extra(ore, i));
        for (String ore : MiningStages.ORE_NAMES) for (int i = 1; i <= STAGED_VARIANTS; i++) for (int st = 1; st <= 3; st++) event.register(stage(ore, i, st));
    }

    private static BakedModel[][] stages(Map<ModelResourceLocation, BakedModel> models, String ore) {
        BakedModel[][] out = new BakedModel[8][4];
        for (int i = 0; i < STAGED_VARIANTS; i++) for (int st = 1; st <= 3; st++) out[i][st] = models.get(stage(ore, i + 1, st));
        return out;
    }

    @SubscribeEvent
    public static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        wrap(models, ModBlocks.RUBI_ORE.get());
        wrap(models, ModBlocks.RUBI_DEEPSLATE_ORE.get());
        wrap(models, ModBlocks.SAPPHIRE_ORE.get());
        wrap(models, ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        wrap(models, ModBlocks.JADE_ORE.get());
        wrap(models, ModBlocks.JADE_DEEPSLATE_ORE.get());
        wrap(models, ModBlocks.FORGE_ORE.get());
        for (String ore : VANILLA_ORES) wrapVanilla(models, ore);
    }

    private static void wrap(Map<ModelResourceLocation, BakedModel> models, Block block) {
        BakedModel[] variants = new BakedModel[8];
        for (int i = 0; i < 8; i++) {
            BlockState s = block.defaultBlockState().setValue(VariantOreBlock.VARIANT, i);
            variants[i] = models.get(BlockModelShaper.stateToModelLocation(s));
            if (variants[i] == null) return; // modelo faltando: deixa tudo como esta (variante 0)
        }
        models.put(BlockModelShaper.stateToModelLocation(block.defaultBlockState()), new RubiOreVariantModel(variants, stages(models, modelName(block))));
    }

    private static void wrapVanilla(Map<ModelResourceLocation, BakedModel> models, String ore) {
        BakedModel[] variants = new BakedModel[8];
        for (int i = 0; i < 8; i++) {
            variants[i] = models.get(extra(ore, i + 1));
            if (variants[i] == null) return;
        }
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(ore));
        RubiOreVariantModel model = new RubiOreVariantModel(variants, stages(models, ore));
        for (BlockState s : block.getStateDefinition().getPossibleStates())
            models.put(BlockModelShaper.stateToModelLocation(s), model);
    }
}
