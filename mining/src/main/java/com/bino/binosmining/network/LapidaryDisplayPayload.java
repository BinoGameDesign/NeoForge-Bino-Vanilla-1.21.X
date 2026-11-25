package com.bino.binosmining.network;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// servidor -> clientes por perto: o que esta em cima de uma mesa de lapidacao (silex na ponta do lapidador, minerio
// no disco), quem esta usando (id da entidade, -1 = ninguem) e se acabou de comecar uma lapidacao (cut: 0 nao,
// 1 uma, 2 tudo). Os outros jogadores desenham a mesa e a pose de quem esta lapidando a partir disso.
public record LapidaryDisplayPayload(BlockPos pos, int user, ItemStack flint, ItemStack ore, int cut) implements CustomPacketPayload {
    public static final Type<LapidaryDisplayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "lapidary_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LapidaryDisplayPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LapidaryDisplayPayload::pos,
            ByteBufCodecs.VAR_INT, LapidaryDisplayPayload::user,
            ItemStack.OPTIONAL_STREAM_CODEC, LapidaryDisplayPayload::flint,
            ItemStack.OPTIONAL_STREAM_CODEC, LapidaryDisplayPayload::ore,
            ByteBufCodecs.VAR_INT, LapidaryDisplayPayload::cut,
            LapidaryDisplayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
