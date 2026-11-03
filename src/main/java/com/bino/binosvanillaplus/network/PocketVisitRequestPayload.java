package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// servidor -> dono: "este jogador quer visitar a sua dimensao" (abre o menu aceitar/recusar)
public record PocketVisitRequestPayload(UUID visitor, String name) implements CustomPacketPayload {
    public static final Type<PocketVisitRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pocket_visit_request"));
    public static final StreamCodec<ByteBuf, PocketVisitRequestPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PocketVisitRequestPayload::visitor,
            ByteBufCodecs.STRING_UTF8, PocketVisitRequestPayload::name,
            PocketVisitRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
