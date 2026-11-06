package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: este villager e' um Pillager civil (para a textura)
public record CivilianSyncPayload(int entity, boolean civilian) implements CustomPacketPayload {
    public static final Type<CivilianSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "civilian_sync"));
    public static final StreamCodec<ByteBuf, CivilianSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CivilianSyncPayload::entity,
            ByteBufCodecs.BOOL, CivilianSyncPayload::civilian,
            CivilianSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
