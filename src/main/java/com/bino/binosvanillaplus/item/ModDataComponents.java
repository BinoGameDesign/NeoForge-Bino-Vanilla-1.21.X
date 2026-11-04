package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// "Componentes" são dados extras que um item carrega. Aqui: o nível de forja (+1 a +10) de um equipamento.
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BinosVanillaMod.MOD_ID);

    // persistent = é salvo no mundo; networkSynchronized = o servidor envia ao jogador (para a dica do item)
    public static final Supplier<DataComponentType<Integer>> FORGE_LEVEL = COMPONENTS.register("forge_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // Chave Dimensional: nivel (upgrades recebidos) e minerios ja usados (nao podem repetir)
    public static final Supplier<DataComponentType<Integer>> KEY_LEVEL = COMPONENTS.register("key_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final Supplier<DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>> KEY_ORES =
            COMPONENTS.register("key_ores",
                    () -> DataComponentType.<java.util.List<net.minecraft.resources.ResourceLocation>>builder()
                            .persistent(net.minecraft.resources.ResourceLocation.CODEC.listOf())
                            .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    // Lanterna: ligada/desligada (na lanterna e no capacete com lanterna) e se o capacete tem a lanterna acoplada
    public static final Supplier<DataComponentType<Boolean>> FLASHLIGHT_ON = COMPONENTS.register("flashlight_on",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final Supplier<DataComponentType<Boolean>> HELMET_LAMP = COMPONENTS.register("helmet_lamp",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    // Foto revelada: a imagem (PNG pequeno, pixelado) e o dia do mundo em que foi tirada
    public record PhotoData(byte[] png, int day) {
        public static final Codec<PhotoData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                Codec.BYTE_BUFFER.xmap(b -> { byte[] a = new byte[b.remaining()]; b.get(a); return a; }, java.nio.ByteBuffer::wrap)
                        .fieldOf("png").forGetter(PhotoData::png),
                Codec.INT.fieldOf("day").forGetter(PhotoData::day)).apply(i, PhotoData::new));
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, PhotoData> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        ByteBufCodecs.byteArray(65536), PhotoData::png,
                        ByteBufCodecs.VAR_INT, PhotoData::day,
                        PhotoData::new);

        @Override public boolean equals(Object o) { return o instanceof PhotoData p && p.day == day && java.util.Arrays.equals(p.png, png); }
        @Override public int hashCode() { return java.util.Arrays.hashCode(png) * 31 + day; }
    }

    public static final Supplier<DataComponentType<PhotoData>> PHOTO = COMPONENTS.register("photo",
            () -> DataComponentType.<PhotoData>builder().persistent(PhotoData.CODEC).networkSynchronized(PhotoData.STREAM_CODEC).build());

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
