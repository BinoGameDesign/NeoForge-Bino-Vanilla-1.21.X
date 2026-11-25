package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
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
//   STAMINA        a = jogador, f = estamina de combate devolvida (negativo = enche tudo)
//   BLOCKED        a = jogador, b = atacante, f = custo de estamina do bloqueio
//   PLAYER_POSTURE a = jogador, b = 1 guarda quebrada, f = 0..1
//   ISSEN_CHAIN    a = jogador, b = mob, f = ms da janela do proximo Issen (abre daqui a pouco, ver CombatActionsServer)
//   CLASH          a = jogador, b = o outro jogador (choque de espadas)
//   HIT_FROM       a = jogador atingido, f = yaw do mundo de onde veio o golpe (aviso na mira)
//   ISSEN_JUMP     a = jogador, b = proximo mob, f = ms da janela (Issen em cadeia: matou -> teleporta para outro mob)
//   ISSEN_WARPED   a = jogador, b = mob, f = elo da cadeia (o jogador acabou de teleportar e cortar)
public record CombatFxPayload(byte kind, int a, int b, float f) implements CustomPacketPayload {
    public static final byte TELEGRAPH = 0, PARRY = 1, ISSEN = 2, PERFECT_DODGE = 3, RUSH = 4, POSTURE = 5, CRITICAL = 6, DEATH = 7,
            STAMINA = 8, BLOCKED = 9, PLAYER_POSTURE = 10, ISSEN_CHAIN = 11, CLASH = 12, HIT_FROM = 13, ISSEN_JUMP = 14, ISSEN_WARPED = 15;
    public static final Type<CombatFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinosCombat.MOD_ID, "combat_fx"));
    public static final StreamCodec<ByteBuf, CombatFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, CombatFxPayload::kind, ByteBufCodecs.VAR_INT, CombatFxPayload::a,
            ByteBufCodecs.VAR_INT, CombatFxPayload::b, ByteBufCodecs.FLOAT, CombatFxPayload::f, CombatFxPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // devolve estamina de combate ao jogador (amount < 0 = enche tudo)
    public static void stamina(net.minecraft.server.level.ServerPlayer p, float amount) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new CombatFxPayload(STAMINA, p.getId(), 0, amount));
    }
}
