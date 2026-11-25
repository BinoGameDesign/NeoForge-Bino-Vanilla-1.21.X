package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import com.bino.binoscombat.CombatParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.bino.binoscombat.CombatSounds;

// CHOQUE DE ESPADAS (PvP, regras de 23/11): quando dois jogadores, de frente um para o outro, golpeiam com menos de
// 0,15 s (3 ticks) de diferenca, as laminas se chocam: ninguem leva dano, os dois sao empurrados para tras, saem
// faiscas e a postura dos dois enche.
@EventBusSubscriber(modid = BinosCombat.MOD_ID)
public final class PvpClash {
    private PvpClash() {}

    private static final Map<UUID, Long> SWING = new HashMap<>();   // jogador -> tick do ultimo golpe que comecou
    private static final Map<UUID, Long> IMMUNE = new HashMap<>();  // depois do choque: o golpe do outro tambem nao entra

    // golpe do combo comecou (CombatMovesServer.onMove)
    static void swing(ServerPlayer p) { SWING.put(p.getUUID(), p.level().getGameTime()); }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) { if (e.getEntity() instanceof ServerPlayer p) swing(p); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer b) || !(e.getSource().getDirectEntity() instanceof ServerPlayer a) || a == b) return;
        long now = b.level().getGameTime();
        Long im = IMMUNE.get(b.getUUID());
        if (im != null && now <= im) { e.setCanceled(true); return; }
        Long sb = SWING.get(b.getUUID()), sa = SWING.get(a.getUUID());
        if (sb == null || sa == null || Math.abs(sb - sa) > 3 || now - sb > 12) return;
        Vec3 ab = b.position().subtract(a.position()).multiply(1, 0, 1);
        if (ab.lengthSqr() < 1e-4) return;
        ab = ab.normalize();
        if (a.getLookAngle().multiply(1, 0, 1).normalize().dot(ab) < 0.5 || b.getLookAngle().multiply(1, 0, 1).normalize().dot(ab.reverse()) < 0.5) return;
        e.setCanceled(true);
        IMMUNE.put(a.getUUID(), now + 4);
        IMMUNE.put(b.getUUID(), now + 4);
        SWING.remove(a.getUUID()); SWING.remove(b.getUUID());
        a.knockback(0.6, ab.x, ab.z); a.hurtMarked = true;
        b.knockback(0.6, -ab.x, -ab.z); b.hurtMarked = true;
        PlayerDefense.addPosture(a, 20f);
        PlayerDefense.addPosture(b, 20f);
        ServerLevel level = b.serverLevel();
        Vec3 mid = a.getEyePosition().add(b.getEyePosition()).scale(0.5).add(0, -0.3, 0);
        level.sendParticles(CombatParticles.PARRY_SPARK.get(), mid.x, mid.y, mid.z, 26, 0.05, 0.05, 0.05, 0.5);
        level.sendParticles(CombatParticles.IMPACT_STAR.get(), mid.x, mid.y, mid.z, 0, 1.4, 1, 0, 1.0);
        level.playSound(null, mid.x, mid.y, mid.z, CombatSounds.PARRY.get(), SoundSource.PLAYERS, 1.3f, 0.9f);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4f, 1.9f);
        PacketDistributor.sendToPlayer(a, new CombatFxPayload(CombatFxPayload.CLASH, a.getId(), b.getId(), 0));
        PacketDistributor.sendToPlayer(b, new CombatFxPayload(CombatFxPayload.CLASH, b.getId(), a.getId(), 0));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { SWING.remove(e.getEntity().getUUID()); IMMUNE.remove(e.getEntity().getUUID()); }
}
