package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> quem ligou o timer: o tripe disparou; o cliente tira a foto do ponto de vista do tripe
public record TripodShotPayload(BlockPos pos, float yaw) implements CustomPacketPayload {
    public static final Type<TripodShotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "tripod_shot"));
    public static final StreamCodec<ByteBuf, TripodShotPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TripodShotPayload::pos,
            ByteBufCodecs.FLOAT, TripodShotPayload::yaw,
            TripodShotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
