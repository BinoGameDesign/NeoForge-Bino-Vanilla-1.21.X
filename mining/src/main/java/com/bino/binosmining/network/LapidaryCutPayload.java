package com.bino.binosmining.network;

import com.bino.binosmining.BinosMining;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "comecei a lapidar nesta mesa" (all = Shift, lapidar tudo). So serve para os outros jogadores
// verem a animacao; quem gasta os itens e' o botao do menu (LapidaryMenu.clickMenuButton) no fim da animacao.
public record LapidaryCutPayload(BlockPos pos, boolean all) implements CustomPacketPayload {
    public static final Type<LapidaryCutPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "lapidary_cut"));
    public static final StreamCodec<ByteBuf, LapidaryCutPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LapidaryCutPayload::pos, ByteBufCodecs.BOOL, LapidaryCutPayload::all, LapidaryCutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
