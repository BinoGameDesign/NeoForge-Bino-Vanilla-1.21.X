package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// action: START (começou a cair), LAND (tocou o chão, com a altura), CANCEL (caiu na água/lava)
public record CyalumeStatePayload(int action, float height) implements CustomPacketPayload {

    public static final int START = 0;
    public static final int LAND = 1;
    public static final int CANCEL = 2;

    public static final Type<CyalumeStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cyalume_state"));

    // Como converter a mensagem em bytes e de volta
    public static final StreamCodec<ByteBuf, CyalumeStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CyalumeStatePayload::action,
            ByteBufCodecs.FLOAT, CyalumeStatePayload::height,
            CyalumeStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}