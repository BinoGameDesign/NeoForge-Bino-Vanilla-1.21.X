package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "comecei este golpe" (o leve vem logo antes do ataque normal do jogo; o pesado faz o proprio golpe)
public record MovePayload(byte move, boolean heavy, byte step) implements CustomPacketPayload {
    public static final Type<MovePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_move"));
    public static final StreamCodec<ByteBuf, MovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, MovePayload::move, ByteBufCodecs.BOOL, MovePayload::heavy, ByteBufCodecs.BYTE, MovePayload::step, MovePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
