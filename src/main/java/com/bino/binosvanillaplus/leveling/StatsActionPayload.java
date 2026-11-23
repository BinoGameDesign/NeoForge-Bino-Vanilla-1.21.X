package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: subir de nivel (kind 0) ou colocar um ponto num atributo (kind 1, stat = PlayerStats.STR/VIT/AGI)
public record StatsActionPayload(byte kind, byte stat) implements CustomPacketPayload {
    public static final Type<StatsActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "stats_action"));
    public static final StreamCodec<ByteBuf, StatsActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, StatsActionPayload::kind, ByteBufCodecs.BYTE, StatsActionPayload::stat, StatsActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
