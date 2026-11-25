package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> outros jogadores: "esse jogador comecou este golpe" (para verem a animacao em terceira pessoa)
public record MoveBroadcastPayload(int entityId, byte move, boolean heavy, int lengthMs) implements CustomPacketPayload {
    public static final Type<MoveBroadcastPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosCombat.MOD_ID, "combat_move_seen"));
    public static final StreamCodec<ByteBuf, MoveBroadcastPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoveBroadcastPayload::entityId, ByteBufCodecs.BYTE, MoveBroadcastPayload::move,
            ByteBufCodecs.BOOL, MoveBroadcastPayload::heavy, ByteBufCodecs.VAR_INT, MoveBroadcastPayload::lengthMs, MoveBroadcastPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
