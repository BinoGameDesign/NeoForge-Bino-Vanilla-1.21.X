package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: "pet, aperte este bloco" (Alt + botao direito em botao, alavanca, porta...). face = Direction.get3DDataValue()
public record PetActivatePayload(BlockPos pos, int face) implements CustomPacketPayload {
    public static final Type<PetActivatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pet_activate"));
    public static final StreamCodec<ByteBuf, PetActivatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PetActivatePayload::pos,
            ByteBufCodecs.VAR_INT, PetActivatePayload::face,
            PetActivatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
