package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.VariantOreBlock;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

// Troca o modelo "variant=0" dos dois minerios por um que escolhe entre os 8 modelos (ver RubiOreVariantModel).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class RubiOreVariantEvents {

    @SubscribeEvent
    public static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
        wrap(event.getModels(), ModBlocks.RUBI_ORE.get());
        wrap(event.getModels(), ModBlocks.RUBI_DEEPSLATE_ORE.get());
    }

    private static void wrap(Map<ModelResourceLocation, BakedModel> models, Block block) {
        BakedModel[] variants = new BakedModel[8];
        for (int i = 0; i < 8; i++) {
            BlockState s = block.defaultBlockState().setValue(VariantOreBlock.VARIANT, i);
            variants[i] = models.get(BlockModelShaper.stateToModelLocation(s));
            if (variants[i] == null) return; // modelo faltando: deixa tudo como esta (variante 0)
        }
        models.put(BlockModelShaper.stateToModelLocation(block.defaultBlockState()), new RubiOreVariantModel(variants));
    }
}
