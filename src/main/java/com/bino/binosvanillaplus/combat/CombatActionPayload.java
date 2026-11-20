package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// cliente -> servidor: acoes especiais do combate (target = id do mob; flag = parry perfeito)
public record CombatActionPayload(byte kind, int target, boolean flag) implements CustomPacketPayload {
    public static final byte PARRY = 0, ISSEN = 1, RUSH = 2, CRITICAL = 3, BACKSTAB = 4;
    public static final Type<CombatActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_action"));
    public static final StreamCodec<ByteBuf, CombatActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, CombatActionPayload::kind, ByteBufCodecs.VAR_INT, CombatActionPayload::target,
            ByteBufCodecs.BOOL, CombatActionPayload::flag, CombatActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
