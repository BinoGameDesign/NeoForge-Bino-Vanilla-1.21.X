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
import com.bino.binosvanillaplus.network.CameraModePayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    private static boolean lastFront = false;   // último estado enviado ao servidor

    // A cada tick do cliente: se você trocou para (ou saiu de) a câmera frontal, avisa o servidor
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { lastFront = false; return; }   // fora de um mundo: reseta

        boolean front = mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT;
        if (front != lastFront) {
            lastFront = front;
            PacketDistributor.sendToServer(new CameraModePayload(front));
        }
    }

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