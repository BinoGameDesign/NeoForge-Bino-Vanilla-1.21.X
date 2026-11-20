package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> clientes: numero de dano em cima de uma entidade (tipo = cor/texto, ver CombatEvents.KIND_*)
public record DamageNumberPayload(int entityId, float amount, byte kind) implements CustomPacketPayload {
    public static final Type<DamageNumberPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "damage_number"));
    public static final StreamCodec<ByteBuf, DamageNumberPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DamageNumberPayload::entityId,
            ByteBufCodecs.FLOAT, DamageNumberPayload::amount,
            ByteBufCodecs.BYTE, DamageNumberPayload::kind,
            DamageNumberPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
