package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> jogadores no alcance do apito: quem apitou (id da entidade) e onde estava naquele momento
public record WhistleSignalPayload(int entityId, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<WhistleSignalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "whistle_signal"));
    public static final StreamCodec<ByteBuf, WhistleSignalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WhistleSignalPayload::entityId,
            ByteBufCodecs.DOUBLE, WhistleSignalPayload::x,
            ByteBufCodecs.DOUBLE, WhistleSignalPayload::y,
            ByteBufCodecs.DOUBLE, WhistleSignalPayload::z,
            WhistleSignalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
