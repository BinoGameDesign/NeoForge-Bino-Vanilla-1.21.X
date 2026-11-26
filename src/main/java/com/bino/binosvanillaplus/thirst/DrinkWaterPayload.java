package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "quero beber da agua que estou olhando" (o servidor confere tudo de novo)
public record DrinkWaterPayload() implements CustomPacketPayload {
    public static final DrinkWaterPayload INSTANCE = new DrinkWaterPayload();
    public static final Type<DrinkWaterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "drink_water"));
    public static final StreamCodec<ByteBuf, DrinkWaterPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
