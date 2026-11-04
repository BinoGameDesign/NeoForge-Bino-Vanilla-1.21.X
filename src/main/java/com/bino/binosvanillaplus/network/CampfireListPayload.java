package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// servidor -> jogador (ao morrer): as fogueiras dele, para escolher onde renascer
public record CampfireListPayload(List<Entry> points) implements CustomPacketPayload {
    public record Entry(String dim, BlockPos pos, boolean valid) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::dim,
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.BOOL, Entry::valid,
                Entry::new);
    }

    public static final Type<CampfireListPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "campfire_list"));
    public static final StreamCodec<ByteBuf, CampfireListPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.CODEC.apply(ByteBufCodecs.list()), CampfireListPayload::points, CampfireListPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
