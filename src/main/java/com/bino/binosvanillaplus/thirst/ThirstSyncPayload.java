package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: quanta sede o jogador tem (0..20)
public record ThirstSyncPayload(int level) implements CustomPacketPayload {
    public static final Type<ThirstSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "thirst_sync"));
    public static final StreamCodec<ByteBuf, ThirstSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThirstSyncPayload::level, ThirstSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
