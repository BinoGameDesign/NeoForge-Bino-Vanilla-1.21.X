package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// servidor -> cliente: acontecimentos do combate para efeitos e estado na tela
//   TELEGRAPH      a = mob, b = alvo, f = ms ate o golpe        (aviso dourado: da para aparar)
//   PARRY          a = jogador, b = mob, f = 1 perfeito / 0 normal
//   ISSEN          a = jogador, b = mob
//   PERFECT_DODGE  a = jogador, b = mob                          (abre a Rajada)
//   RUSH           a = jogador, b = mob
//   POSTURE        a = mob, b = 1 guarda quebrada, f = 0..1
//   CRITICAL       a = jogador, b = mob
//   DEATH          a = mob, b = 1 morte com stab, f = direcao do golpe (graus, atan2(dz, dx))
public record CombatFxPayload(byte kind, int a, int b, float f) implements CustomPacketPayload {
    public static final byte TELEGRAPH = 0, PARRY = 1, ISSEN = 2, PERFECT_DODGE = 3, RUSH = 4, POSTURE = 5, CRITICAL = 6, DEATH = 7;
    public static final Type<CombatFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_fx"));
    public static final StreamCodec<ByteBuf, CombatFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, CombatFxPayload::kind, ByteBufCodecs.VAR_INT, CombatFxPayload::a,
            ByteBufCodecs.VAR_INT, CombatFxPayload::b, ByteBufCodecs.FLOAT, CombatFxPayload::f, CombatFxPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
