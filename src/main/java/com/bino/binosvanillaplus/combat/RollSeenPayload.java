package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> quem ve: "esse jogador rolou" (para a animacao em terceira pessoa)
public record RollSeenPayload(int entityId, float yaw, boolean back) implements CustomPacketPayload {
    public static final Type<RollSeenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_roll_seen"));
    public static final StreamCodec<ByteBuf, RollSeenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RollSeenPayload::entityId, ByteBufCodecs.FLOAT, RollSeenPayload::yaw,
            ByteBufCodecs.BOOL, RollSeenPayload::back, RollSeenPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
