package com.bino.binosvanillaplus.client.goblin;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class GoblinClientEvents {
    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CaveGoblinModel.LAYER, CaveGoblinModelData::createBodyLayer);
        e.registerLayerDefinition(com.bino.binosvanillaplus.client.rat.RatModel.LAYER, com.bino.binosvanillaplus.client.rat.RatModelData::createBodyLayer);
    }
}
