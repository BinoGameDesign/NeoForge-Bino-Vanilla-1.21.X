package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    // Ensina o jogo a desenhar a entidade: o ícone do item, sempre iluminado (true = brilho total)
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CYALUME_STICK.get(),
                context -> new ThrownItemRenderer<>(context, 1.0f, true));

        // Usa o renderizador original do Allay (modelo e animação vanilla)
        event.<Allay>registerEntityRenderer(ModEntities.COMPANION_ALLAY.get(), AllayRenderer::new);
    }


    // Registra a camada da tela que mostra o cronômetro
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cyalume_timer"),
                CyalumeHud::render);
    }

}