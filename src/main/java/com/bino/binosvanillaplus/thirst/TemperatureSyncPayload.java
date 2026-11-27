package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: temperatura do ambiente (°C) e a exposicao (-100 frio .. +100 calor)
public record TemperatureSyncPayload(int ambient, int exposure) implements CustomPacketPayload {
    public static final Type<TemperatureSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "temperature_sync"));
    public static final StreamCodec<ByteBuf, TemperatureSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TemperatureSyncPayload::ambient, ByteBufCodecs.VAR_INT, TemperatureSyncPayload::exposure, TemperatureSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
