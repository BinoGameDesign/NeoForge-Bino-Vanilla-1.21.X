package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: nivel de um mob (para o painel de inspecao e o mapa)
public record MobLevelPayload(int entityId, int level) implements CustomPacketPayload {
    public static final Type<MobLevelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mob_level"));
    public static final StreamCodec<ByteBuf, MobLevelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobLevelPayload::entityId,
            ByteBufCodecs.VAR_INT, MobLevelPayload::level,
            MobLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
