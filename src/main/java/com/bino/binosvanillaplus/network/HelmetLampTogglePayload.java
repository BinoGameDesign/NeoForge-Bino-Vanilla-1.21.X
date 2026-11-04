package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: liga/desliga a lanterna do capacete (tecla)
public record HelmetLampTogglePayload() implements CustomPacketPayload {
    public static final HelmetLampTogglePayload INSTANCE = new HelmetLampTogglePayload();
    public static final Type<HelmetLampTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "helmet_lamp_toggle"));
    public static final StreamCodec<ByteBuf, HelmetLampTogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
