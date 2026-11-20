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
// Mobs fortes as vezes fazem um ataque VERMELHO: nao da para aparar, so esquivar. O mimico usa startCustom/parriedCustom.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class Telegraphs {
    private Telegraphs() {}

    public static final int WINDUP = 11;   // 0,55 s (cabe a janela de cores do parry, ParryZones)

    record Claim(int mobId, int zone, long tick) {}
    private static final Map<UUID, Claim> CLAIMS = new HashMap<>();

    public static int grace(ServerPlayer p) {
        return Math.max(0, Math.min(4, Math.round(p.connection.latency() / 50f)));
    }

    // comecou a preparacao: avisa quem esta vendo o mob (tempo negativo = ataque VERMELHO, nao da parry)
    public static void start(Mob mob, ServerPlayer target, boolean red) { start(mob, target, WINDUP, red); }

    public static void start(Mob mob, ServerPlayer target, int windupTicks, boolean red) {
        float ms = windupTicks * 50f;
        PacketDistributor.sendToPlayersTrackingEntity(mob, new CombatFxPayload(CombatFxPayload.TELEGRAPH, mob.getId(), target.getId(), red ? -ms : ms));
    }

    // ataque vermelho (so esquiva): mobs fortes (nivel 20+ ou 50+ de vida maxima), 30% das vezes
    public static boolean rollRed(Mob mob) {
        boolean strong = com.bino.binosvanillaplus.leveling.MobLevels.get(mob) >= 20 || mob.getMaxHealth() >= 50;
        return strong && mob.getRandom().nextFloat() < 0.3f;
    }

    // ---- mobs com ataque proprio (mimico): comeca o aviso e confere o parry na hora da mordida
    private static final Map<UUID, Long> CUSTOM = new HashMap<>();

    public static void startCustom(Mob mob, LivingEntity target, int windupTicks) {
        if (!eligible(target)) return;
        CUSTOM.put(mob.getUUID(), mob.level().getGameTime());
        start(mob, (ServerPlayer) target, windupTicks, false);
    }

    // true = foi aparado (nao morde)
    public static boolean parriedCustom(Mob mob, LivingEntity target) {
        Long start = CUSTOM.remove(mob.getUUID());
        return start != null && eligible(target) && tryParry(mob, (ServerPlayer) target, start, false);
    }

    // o jogador apertou parry contra este mob
    public static void claim(ServerPlayer p, int mobId, int zone) {
        CLAIMS.put(p.getUUID(), new Claim(mobId, zone, p.level().getGameTime()));
    }

    // hora do golpe: foi aparado? (o aperto tem que ter vindo depois do comeco da preparacao)
    public static boolean tryParry(Mob mob, ServerPlayer target, long windupStart, boolean red) {
        Claim c = CLAIMS.get(target.getUUID());
        if (c == null || c.mobId() != mob.getId() || c.tick() < windupStart - 1) return false;
        CLAIMS.remove(target.getUUID());
        if (red) return false;   // ataque vermelho nao se apara
        CombatActionsServer.parried(target, mob, c.zone());
        return true;
    }

    public static boolean eligible(LivingEntity target) { return target instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator(); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { CLAIMS.remove(e.getEntity().getUUID()); if (CUSTOM.size() > 256) CUSTOM.clear(); }
}
