package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "fiz um ping neste bloco" (Alt + botao esquerdo)
public record PingPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "ping"));
    public static final StreamCodec<ByteBuf, PingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PingPayload::pos, PingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
