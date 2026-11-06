package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: faccao escolhida na tela. Servidor -> cliente (faction = -1): abre a tela de escolha.
public record FactionChoosePayload(int faction) implements CustomPacketPayload {
    public static final Type<FactionChoosePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "faction_choose"));
    public static final StreamCodec<ByteBuf, FactionChoosePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FactionChoosePayload::faction, FactionChoosePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
