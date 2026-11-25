package com.bino.binosvanillaplus.client.trap;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

// Ratoeira e gaiola: pecas extras dos modelos e os renderizadores
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class TrapClientEvents {
    private TrapClientEvents() {}

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(RatTrapRenderer.BAIL);   // arco da ratoeira
        event.register(CageRenderer.DOOR);      // porta da gaiola
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RAT_TRAP.get(), RatTrapRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CAGE.get(), CageRenderer::new);
    }
}
