package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// jogador -> servidor: renascer na fogueira 'index' (o renascer normal do jogo vem logo depois)
public record RespawnChoicePayload(int index) implements CustomPacketPayload {
    public static final Type<RespawnChoicePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "respawn_choice"));
    public static final StreamCodec<ByteBuf, RespawnChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RespawnChoicePayload::index, RespawnChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
