package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.CyalumeClientState;
import com.bino.binosvanillaplus.entity.CompanionAllayManager;
import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import com.bino.binosvanillaplus.entity.PetInteractables;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

        // "playToServer" = mensagem que o cliente manda para o servidor
        registrar.playToServer(CameraModePayload.TYPE, CameraModePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CompanionAllayManager.setFrontCamera(context.player().getUUID(), payload.front())));

        // ping (Alt + botao esquerdo): o servidor repassa para todos da mesma dimensao
        registrar.playToServer(PingPayload.TYPE, PingPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> onPing((ServerPlayer) context.player(), payload)));
        registrar.playToClient(PingBroadcastPayload.TYPE, PingBroadcastPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.map.Pings.receive(payload)));

        // Dimensao Portatil: pedido de visita (servidor -> dono) e resposta do dono (dono -> servidor)
        registrar.playToClient(PocketVisitRequestPayload.TYPE, PocketVisitRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.pocket.PocketVisitScreen.receive(payload)));
        registrar.playToServer(PocketVisitReplyPayload.TYPE, PocketVisitReplyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.bino.binosvanillaplus.pocket.PocketManager.reply((ServerPlayer) context.player(), payload.visitor(), payload.accept())));

        // tumulo: marca/limpa o ponto de morte no mapa
        registrar.playToClient(GraveMarkPayload.TYPE, GraveMarkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.map.MapWaypoints.onGraveMark(payload)));

        // pet aperta um bloco (Alt + botao direito)
        registrar.playToServer(PetActivatePayload.TYPE, PetActivatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> onPetActivate((ServerPlayer) context.player(), payload)));
    }

    private static final Map<UUID, Long> LAST_PING = new HashMap<>();

    private static void onPing(ServerPlayer sp, PingPayload p) {
        long now = sp.serverLevel().getGameTime();
        Long last = LAST_PING.get(sp.getUUID());
        if (last != null && now - last < 6) return;   // no maximo ~3 pings por segundo
        LAST_PING.put(sp.getUUID(), now);
        if (p.pos().distSqr(sp.blockPosition()) > 300.0 * 300.0) return;
        int color = 0xFFFFFF;   // ping branco
        PacketDistributor.sendToPlayersInDimension(sp.serverLevel(), new PingBroadcastPayload(p.pos(), sp.getName().getString(), color));
    }

    private static void onPetActivate(ServerPlayer sp, PetActivatePayload p) {
        if (!sp.serverLevel().isLoaded(p.pos()) || p.pos().distSqr(sp.blockPosition()) > 32.0 * 32.0) return;
        if (!PetInteractables.allowed(sp.serverLevel().getBlockState(p.pos()))) return;
        CompanionAllayEntity pet = CompanionAllayManager.getPet(sp);
        if (pet != null) pet.startTask(p.pos(), Direction.from3DDataValue(p.face()));
    }
}