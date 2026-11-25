package com.bino.binoscombat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// Mensagens do Combat entre cliente e servidor
@EventBusSubscriber(modid = BinosCombat.MOD_ID)
public final class CombatNetwork {
    private CombatNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");   // "1" = versao do protocolo

        // mod Combat: numeros de dano (servidor -> clientes) e golpe no ar/agua (cliente -> servidor)
        registrar.playToClient(com.bino.binoscombat.combat.DamageNumberPayload.TYPE, com.bino.binoscombat.combat.DamageNumberPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.client.combat.DamageNumbers.receive(payload)));
        registrar.playToServer(com.bino.binoscombat.combat.SwingPayload.TYPE, com.bino.binoscombat.combat.SwingPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.combat.CombatEvents.onSwing((ServerPlayer) context.player(), payload)));

        // mod Combat: golpes do combo (cliente -> servidor) e animacao dos outros jogadores (servidor -> clientes)
        registrar.playToServer(com.bino.binoscombat.combat.MovePayload.TYPE, com.bino.binoscombat.combat.MovePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.combat.CombatMovesServer.onMove((ServerPlayer) context.player(), payload)));
        registrar.playToClient(com.bino.binoscombat.combat.MoveBroadcastPayload.TYPE, com.bino.binoscombat.combat.MoveBroadcastPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.client.combat.CombatMoves.receive(payload)));
        registrar.playToServer(com.bino.binoscombat.combat.CombatActionPayload.TYPE, com.bino.binoscombat.combat.CombatActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.combat.CombatActionsServer.onAction((ServerPlayer) context.player(), payload)));
        registrar.playToClient(com.bino.binoscombat.combat.CombatFxPayload.TYPE, com.bino.binoscombat.combat.CombatFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.client.combat.CombatFx.receive(payload)));
        registrar.playToServer(com.bino.binoscombat.combat.RollPayload.TYPE, com.bino.binoscombat.combat.RollPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.combat.DodgeServer.onRoll((ServerPlayer) context.player(), payload)));
        registrar.playToClient(com.bino.binoscombat.combat.RollSeenPayload.TYPE, com.bino.binoscombat.combat.RollSeenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binoscombat.client.combat.DodgeRoll.receive(payload)));
    }
}
