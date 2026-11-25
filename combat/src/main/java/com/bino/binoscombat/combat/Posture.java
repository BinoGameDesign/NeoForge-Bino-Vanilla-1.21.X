package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Postura (mod Combat, estilo Elden Ring) + congelar/desacelerar mobs.
//  - cada mob tem uma barra escondida que enche com golpes, parry e Issen; esvazia devagar sem apanhar por 3 s.
//  - cheia = GUARDA QUEBRADA: o mob fica parado ~2 s (de joelhos no cliente) e aceita o GOLPE CRITICO.
//  - congelar (parry perfeito) e desacelerar (esquiva perfeita) pulam ticks so DAQUELE mob, sem mexer no tempo do
//    mundo (no multiplayer os outros jogadores nao sentem).
// Jogadores nao tem postura.
@EventBusSubscriber(modid = BinosCombat.MOD_ID)
public final class Posture {
    private Posture() {}

    public static final int BREAK_TICKS = 44, DECAY_AFTER = 60;

    static final class P {
        float value; long lastHit, brokenUntil, frozenUntil, slowUntil;
        boolean wasBroken; float lastSent = -1;
    }

    private static final Map<UUID, P> MAP = new HashMap<>();
    private static final Map<UUID, LivingEntity> ENT = new HashMap<>();

    private static boolean boss(LivingEntity e) { return e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden; }

    // postura maxima: vida, armadura, resistencia a empurrao e tamanho (mob mais pesado/forte aguenta mais; regras de 23/11)
    public static float max(LivingEntity e) {
        double armor = e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) != null ? e.getArmorValue() : 0;
        double kb = e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) != null
                ? e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) : 0;
        double size = e.getBbWidth() * e.getBbHeight();
        return (float) (10 + e.getMaxHealth() * 0.6 + armor * 1.2 + kb * 30 + size * 4) * (boss(e) ? 4f : 1f);
    }

    private static P get(LivingEntity e) {
        ENT.put(e.getUUID(), e);
        return MAP.computeIfAbsent(e.getUUID(), k -> new P());
    }

    private static long now(LivingEntity e) { return e.level().getGameTime(); }

    public static void add(LivingEntity e, float amount) {
        if (amount <= 0 || e instanceof Player || !(e.level() instanceof ServerLevel level) || !e.isAlive()) return;
        P p = get(e);
        long now = now(e);
        if (p.brokenUntil > now) return;
        p.value += amount;
        p.lastHit = now;
        float max = max(e);
        if (p.value >= max) {
            p.value = max;
            p.brokenUntil = now + BREAK_TICKS;
            p.wasBroken = true;
            if (e instanceof Mob m) { m.getNavigation().stop(); }
            level.playSound(null, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 1.6f);
            level.playSound(null, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);
        }
        sync(e, p, true);
    }

    public static boolean broken(LivingEntity e) {
        P p = MAP.get(e.getUUID());
        return p != null && p.brokenUntil > now(e);
    }

    public static boolean busy(LivingEntity e) {   // nao pode atacar (quebrado ou congelado)
        P p = MAP.get(e.getUUID());
        if (p == null) return false;
        long now = now(e);
        return p.brokenUntil > now || p.frozenUntil > now;
    }

    public static void freeze(LivingEntity e, int ticks) { if (!(e instanceof Player)) { P p = get(e); p.frozenUntil = Math.max(p.frozenUntil, now(e) + ticks); } }

    public static void slow(LivingEntity e, int ticks) { if (!(e instanceof Player)) { P p = get(e); p.slowUntil = Math.max(p.slowUntil, now(e) + ticks); } }

    // golpe critico: termina a quebra e zera a barra
    public static void endBreak(LivingEntity e) {
        P p = MAP.get(e.getUUID());
        if (p == null) return;
        p.brokenUntil = 0; p.value = 0; p.wasBroken = false;
        sync(e, p, true);
    }

    private static void sync(LivingEntity e, P p, boolean force) {
        float f = p.value / max(e);
        boolean br = p.brokenUntil > now(e);
        if (!force && Math.abs(f - p.lastSent) < 0.02f) return;
        p.lastSent = f;
        PacketDistributor.sendToPlayersTrackingEntity(e, new CombatFxPayload(CombatFxPayload.POSTURE, e.getId(), br ? 1 : 0, f));
    }

    // quebrado/congelado: o mob nao faz nada no tick (mas os tempos de dano continuam correndo, para poder apanhar)
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide) return;
        P p = MAP.get(m.getUUID());
        if (p == null) return;
        long now = now(m);
        boolean stop = p.brokenUntil > now || p.frozenUntil > now || (p.slowUntil > now && now % 3 != 0);
        if (!stop) return;
        if (m.invulnerableTime > 0) m.invulnerableTime--;
        if (m.hurtTime > 0) m.hurtTime--;
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 5 != 0) return;
        for (Iterator<Map.Entry<UUID, P>> it = MAP.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, P> en = it.next();
            LivingEntity ent = ENT.get(en.getKey());
            P p = en.getValue();
            if (ent == null || !ent.isAlive() || ent.isRemoved()) { it.remove(); ENT.remove(en.getKey()); continue; }
            long now = now(ent);
            if (p.wasBroken && p.brokenUntil <= now) {   // acabou a quebra sem critico: volta pela metade
                p.wasBroken = false;
                p.value = max(ent) * 0.5f;
                p.lastHit = now;
                sync(ent, p, true);
            } else if (p.brokenUntil <= now && now - p.lastHit > DECAY_AFTER && p.value > 0) {
                p.value = Math.max(0, p.value - max(ent) * 0.04f);
                sync(ent, p, false);
            }
            if (p.value <= 0 && p.brokenUntil <= now && p.frozenUntil <= now && p.slowUntil <= now) {
                if (p.lastSent > 0) sync(ent, p, true);
                it.remove(); ENT.remove(en.getKey());
            }
        }
    }
}
