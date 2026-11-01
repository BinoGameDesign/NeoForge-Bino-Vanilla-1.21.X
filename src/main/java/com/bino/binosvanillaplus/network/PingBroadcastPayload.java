package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> clientes da mesma dimensao: ping de alguem (nome + cor do jogador)
public record PingBroadcastPayload(BlockPos pos, String name, int color) implements CustomPacketPayload {
    public static final Type<PingBroadcastPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "ping_broadcast"));
    public static final StreamCodec<ByteBuf, PingBroadcastPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PingBroadcastPayload::pos,
            ByteBufCodecs.STRING_UTF8, PingBroadcastPayload::name,
            ByteBufCodecs.INT, PingBroadcastPayload::color,
            PingBroadcastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
