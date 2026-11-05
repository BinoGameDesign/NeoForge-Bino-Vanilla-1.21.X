package com.bino.binosvanillaplus.network;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: OPEN = abrir o inventario novo; SORT_NAME/SORT_COUNT/SORT_ID = organizar os 27 slots
public record InventoryActionPayload(int action) implements CustomPacketPayload {
    public static final int OPEN = 0, SORT_NAME = 1, SORT_COUNT = 2, SORT_ID = 3;
    public static final Type<InventoryActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "inventory_action"));
    public static final StreamCodec<ByteBuf, InventoryActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InventoryActionPayload::action, InventoryActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
