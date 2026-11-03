package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// dono -> servidor: aceitou (true) ou recusou (false) a visita. O servidor so aceita se houver pedido pendente para ELE.
public record PocketVisitReplyPayload(UUID visitor, boolean accept) implements CustomPacketPayload {
    public static final Type<PocketVisitReplyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pocket_visit_reply"));
    public static final StreamCodec<ByteBuf, PocketVisitReplyPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PocketVisitReplyPayload::visitor,
            ByteBufCodecs.BOOL, PocketVisitReplyPayload::accept,
            PocketVisitReplyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
