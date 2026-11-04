package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// servidor -> cliente: o conteudo do diario (descobertas + fotos livres)
public record DiaryDataPayload(List<Entry> entries, List<Pic> photos) implements CustomPacketPayload {
    public record Entry(String key, String name, String desc, String icon, int x, int y, int z, long day) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = new StreamCodec<>() {
            @Override public Entry decode(ByteBuf b) {
                return new Entry(ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b),
                        ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_LONG.decode(b));
            }
            @Override public void encode(ByteBuf b, Entry e) {
                ByteBufCodecs.STRING_UTF8.encode(b, e.key()); ByteBufCodecs.STRING_UTF8.encode(b, e.name());
                ByteBufCodecs.STRING_UTF8.encode(b, e.desc()); ByteBufCodecs.STRING_UTF8.encode(b, e.icon());
                ByteBufCodecs.VAR_INT.encode(b, e.x()); ByteBufCodecs.VAR_INT.encode(b, e.y()); ByteBufCodecs.VAR_INT.encode(b, e.z());
                ByteBufCodecs.VAR_LONG.encode(b, e.day());
            }
        };
    }

    public record Pic(byte[] png, long day) {
        public static final StreamCodec<ByteBuf, Pic> CODEC = StreamCodec.composite(
                ByteBufCodecs.byteArray(PhotoUploadPayload.MAX_BYTES), Pic::png,
                ByteBufCodecs.VAR_LONG, Pic::day,
                Pic::new);
    }

    public static final Type<DiaryDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "diary_data"));
    public static final StreamCodec<ByteBuf, DiaryDataPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.CODEC.apply(ByteBufCodecs.list()), DiaryDataPayload::entries,
            Pic.CODEC.apply(ByteBufCodecs.list()), DiaryDataPayload::photos,
            DiaryDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
