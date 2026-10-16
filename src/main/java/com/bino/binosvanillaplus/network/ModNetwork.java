package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.CyalumeClientState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");   // "1" = versão do protocolo

        // "playToClient" = mensagem que o servidor manda para o cliente
        registrar.playToClient(CyalumeStatePayload.TYPE, CyalumeStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CyalumeClientState.handle(payload)));
    }
}