package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// servidor -> cliente: o item nas costas de um jogador (elytra no slot extra)
public record BackItemPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
    public static final Type<BackItemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "back_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackItemPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC, BackItemPayload::stack,
            BackItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
