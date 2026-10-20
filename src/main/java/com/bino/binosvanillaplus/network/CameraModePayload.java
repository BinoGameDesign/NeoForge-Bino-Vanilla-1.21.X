package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// front = true quando o jogador está na câmera frontal (F5 de frente)
public record CameraModePayload(boolean front) implements CustomPacketPayload {

    public static final Type<CameraModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "camera_mode"));

    public static final StreamCodec<ByteBuf, CameraModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CameraModePayload::front,
            CameraModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}