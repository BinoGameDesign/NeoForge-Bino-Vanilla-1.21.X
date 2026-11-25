package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: golpe de espada/machado que nao acertou nada (water = acertou a agua, em x/y/z).
// O servidor toca o som para os OUTROS jogadores (quem golpeou ja ouviu na hora, no proprio cliente).
public record SwingPayload(boolean water, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<SwingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosCombat.MOD_ID, "combat_swing"));
    public static final StreamCodec<ByteBuf, SwingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SwingPayload::water,
            ByteBufCodecs.DOUBLE, SwingPayload::x,
            ByteBufCodecs.DOUBLE, SwingPayload::y,
            ByteBufCodecs.DOUBLE, SwingPayload::z,
            SwingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
