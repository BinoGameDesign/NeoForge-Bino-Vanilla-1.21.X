package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Aviso de ataque (mod Combat): mobs que atacam corpo a corpo (MeleeAttackGoal: zumbi, aranha, vindicador,
// piglin, lobo...) seguram o golpe por WINDUP ticks antes de bater em um JOGADOR, com um brilho dourado
// (mixin MeleeAttackGoalTelegraphMixin). E' o que torna o parry e a esquiva perfeita justos.
// Com internet lenta o golpe espera tambem o ping do alvo (no maximo 4 ticks), para o aperto do parry chegar a tempo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class Telegraphs {
    private Telegraphs() {}

    public static final int WINDUP = 6;   // 0,3 s

    record Claim(int mobId, boolean perfect, long tick) {}
    private static final Map<UUID, Claim> CLAIMS = new HashMap<>();

    public static int grace(ServerPlayer p) {
        return Math.max(0, Math.min(4, Math.round(p.connection.latency() / 50f)));
    }

    // comecou a preparacao: avisa quem esta vendo o mob
    public static void start(Mob mob, ServerPlayer target) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new CombatFxPayload(CombatFxPayload.TELEGRAPH, mob.getId(), target.getId(), WINDUP * 50f));
    }

    // o jogador apertou parry contra este mob
    public static void claim(ServerPlayer p, int mobId, boolean perfect) {
        CLAIMS.put(p.getUUID(), new Claim(mobId, perfect, p.level().getGameTime()));
    }

    // hora do golpe: foi aparado? (o aperto tem que ter vindo depois do comeco da preparacao)
    public static boolean tryParry(Mob mob, ServerPlayer target, long windupStart) {
        Claim c = CLAIMS.get(target.getUUID());
        if (c == null || c.mobId() != mob.getId() || c.tick() < windupStart - 1) return false;
        CLAIMS.remove(target.getUUID());
        CombatActionsServer.parried(target, mob, c.perfect());
        return true;
    }

    public static boolean eligible(LivingEntity target) { return target instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator(); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { CLAIMS.remove(e.getEntity().getUUID()); }
}
