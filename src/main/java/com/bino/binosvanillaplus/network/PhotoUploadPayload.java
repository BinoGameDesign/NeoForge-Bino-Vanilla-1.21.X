package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: a foto tirada (PNG pequeno). tripod = tirada pelo timer do tripe em tripodPos
public record PhotoUploadPayload(byte[] png, boolean tripod, BlockPos tripodPos) implements CustomPacketPayload {
    public static final int MAX_BYTES = 40000;
    public static final Type<PhotoUploadPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "photo_upload"));
    public static final StreamCodec<ByteBuf, PhotoUploadPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(MAX_BYTES), PhotoUploadPayload::png,
            ByteBufCodecs.BOOL, PhotoUploadPayload::tripod,
            BlockPos.STREAM_CODEC, PhotoUploadPayload::tripodPos,
            PhotoUploadPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
