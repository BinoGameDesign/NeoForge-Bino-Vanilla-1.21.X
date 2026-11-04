package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> dono: marca (ou limpa) o ponto de morte no mapa. GRAVE = tumulo nesse bloco; DEATH = so o lugar da morte
// (sem tumulo); CLEAR = o tumulo foi esvaziado/dissipado.
public record GraveMarkPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int GRAVE = 0, DEATH = 1, CLEAR = 2;
    public static final Type<GraveMarkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "grave_mark"));
    public static final StreamCodec<ByteBuf, GraveMarkPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GraveMarkPayload::pos,
            ByteBufCodecs.VAR_INT, GraveMarkPayload::action,
            GraveMarkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
