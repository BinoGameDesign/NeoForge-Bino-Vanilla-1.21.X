package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: nivel do personagem e os pontos de cada atributo
public record StatsSyncPayload(int level, int str, int vit, int agi, int intel, int wil) implements CustomPacketPayload {
    public static final Type<StatsSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "stats_sync"));
    public static final StreamCodec<ByteBuf, StatsSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatsSyncPayload::level, ByteBufCodecs.VAR_INT, StatsSyncPayload::str,
            ByteBufCodecs.VAR_INT, StatsSyncPayload::vit, ByteBufCodecs.VAR_INT, StatsSyncPayload::agi,
            ByteBufCodecs.VAR_INT, StatsSyncPayload::intel, ByteBufCodecs.VAR_INT, StatsSyncPayload::wil, StatsSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
