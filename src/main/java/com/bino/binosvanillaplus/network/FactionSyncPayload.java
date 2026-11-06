package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: faccao de um jogador (para o nome vermelho dos rivais)
public record FactionSyncPayload(UUID player, int faction) implements CustomPacketPayload {
    public static final Type<FactionSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "faction_sync"));
    public static final StreamCodec<ByteBuf, FactionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FactionSyncPayload::player,
            ByteBufCodecs.VAR_INT, FactionSyncPayload::faction,
            FactionSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
