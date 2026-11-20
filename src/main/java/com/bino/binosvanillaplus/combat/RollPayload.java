package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "rolei" (yaw = direcao do rolamento; back = passo para tras, sem direcao apertada)
public record RollPayload(float yaw, boolean back) implements CustomPacketPayload {
    public static final Type<RollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_roll"));
    public static final StreamCodec<ByteBuf, RollPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RollPayload::yaw, ByteBufCodecs.BOOL, RollPayload::back, RollPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
