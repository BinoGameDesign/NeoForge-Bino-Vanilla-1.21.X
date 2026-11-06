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

        // lanterna do capacete (tecla)
        registrar.playToServer(HelmetLampTogglePayload.TYPE, HelmetLampTogglePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> toggleHelmetLamp((ServerPlayer) context.player())));

        // apito: silhueta de quem apitou
        registrar.playToClient(WhistleSignalPayload.TYPE, WhistleSignalPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.gear.WhistleSignals.receive(payload)));

        // camera, tripe e diario de exploracao
        registrar.playToServer(PhotoUploadPayload.TYPE, PhotoUploadPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.journal.Journal.onPhoto((ServerPlayer) context.player(), payload)));
        registrar.playToServer(DiaryRequestPayload.TYPE, DiaryRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerPlayer sp = (ServerPlayer) context.player();
                    if (payload.develop() >= 0) com.bino.binosvanillaplus.journal.Journal.develop(sp, payload.develop());
                    com.bino.binosvanillaplus.journal.Journal.sendDiary(sp);
                }));
        registrar.playToClient(DiaryDataPayload.TYPE, DiaryDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.journal.DiaryScreen.receive(payload)));
        registrar.playToClient(TripodShotPayload.TYPE, TripodShotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.journal.PhotoCapture.tripod(payload)));

        // fogueiras: lista ao morrer e escolha do lugar para renascer
        registrar.playToClient(CampfireListPayload.TYPE, CampfireListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.campfire.RespawnChoices.receive(payload)));
        registrar.playToServer(RespawnChoicePayload.TYPE, RespawnChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.campfire.Campfires.choose((ServerPlayer) context.player(), payload.index())));

        // faccoes: escolha (tela <-> servidor) e faccao de cada jogador (nome vermelho dos rivais)
        registrar.playBidirectional(FactionChoosePayload.TYPE, FactionChoosePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.flow().isServerbound()) com.bino.binosvanillaplus.faction.FactionEvents.choose((ServerPlayer) context.player(), payload.faction());
                    else com.bino.binosvanillaplus.client.faction.FactionClient.open(payload);
                }));
        registrar.playToClient(FactionSyncPayload.TYPE, FactionSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.faction.FactionClient.receive(payload)));

        // nivel dos mobs (painel de inspecao)
        registrar.playToClient(MobLevelPayload.TYPE, MobLevelPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.client.leveling.LevelClient.receive(payload)));

        // inventario novo: abrir/organizar e item das costas (elytra)
        registrar.playToServer(InventoryActionPayload.TYPE, InventoryActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.inventory.InventoryActions.handle((ServerPlayer) context.player(), payload.action())));
        registrar.playToClient(BackItemPayload.TYPE, BackItemPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.bino.binosvanillaplus.inventory.BackSlot.CLIENT.put(payload.entityId(), payload.stack())));

        // pet aperta um bloco (Alt + botao direito)
        registrar.playToServer(PetActivatePayload.TYPE, PetActivatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> onPetActivate((ServerPlayer) context.player(), payload)));
    }

    private static void toggleHelmetLamp(ServerPlayer sp) {
        net.minecraft.world.item.ItemStack head = sp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (!head.getOrDefault(com.bino.binosvanillaplus.item.ModDataComponents.HELMET_LAMP.get(), false)) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.binosvanillaplus.helmet_lamp.none"), true);
            return;
        }
        boolean on = !head.getOrDefault(com.bino.binosvanillaplus.item.ModDataComponents.FLASHLIGHT_ON.get(), false);
        head.set(com.bino.binosvanillaplus.item.ModDataComponents.FLASHLIGHT_ON.get(), on);
        sp.serverLevel().playSound(null, sp.getX(), sp.getEyeY(), sp.getZ(),
                on ? net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON : net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, on ? 1.6f : 1.3f);
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