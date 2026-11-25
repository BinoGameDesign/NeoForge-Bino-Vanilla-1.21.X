package com.bino.binosmining.network;

import com.bino.binosmining.BinosMining;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BinosMining.MOD_ID)
public class MiningNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // mesa de lapidacao: inicio da animacao (cliente -> servidor) e o que esta em cima da mesa (servidor -> clientes)
        registrar.playToServer(LapidaryCutPayload.TYPE, LapidaryCutPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.bino.binosmining.menu.LapidaryMenu.onCutStarted((ServerPlayer) context.player(), payload.pos(), payload.all())));
        registrar.playToClient(LapidaryDisplayPayload.TYPE, LapidaryDisplayPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosmining.client.lapidary.LapidaryRemote.receive(payload)));
    }
}
