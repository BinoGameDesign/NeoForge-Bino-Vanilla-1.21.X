package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Acoes especiais do combate (mod Combat), servidor:
//  - PARRY: o jogador avisa que aparou um golpe com aviso (Telegraphs decide na hora do golpe).
//      normal  = sem dano, o mob recua, +30% de postura, faisca dourada.
//      perfeito= o mob congela ~0,4 s, +50% de postura, meio coracao de vida, e abre o ISSEN por ~0,5 s.
//  - ISSEN: contra-golpe do parry perfeito: dano x4 e postura cheia; o jogador atravessa o mob (se tiver espaco).
//  - RUSH (Rajada): depois da esquiva perfeita, 5 cortes automaticos (dano x0,6 cada) no mob desacelerado.
//  - CRITICAL: golpe critico no mob de guarda quebrada: dano x3.
//  - Recompensa: morte por Issen ou critico da XP extra.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class CombatActionsServer {
    private CombatActionsServer() {}

    private record Window(int mobId, long until) {}
    private static final Map<UUID, Window> ISSEN = new HashMap<>(), RUSH = new HashMap<>();
    private static final Map<UUID, Long> FINISHED_BY_SPECIAL = new HashMap<>();   // mob -> tick do golpe especial

    private record RushHit(UUID player, int mobId, long at, int left) {}
    private record StabHit(UUID player, int mobId, long at, boolean axe) {}
    private record ArrowParry(long until, boolean perfect) {}
    private static final Map<UUID, ArrowParry> ARROWS = new HashMap<>();

    // flecha (ou outro projetil) chegando em quem esta aparando, de frente: rebate. Perfeito = volta para quem atirou
    @SubscribeEvent
    public static void onProjectile(net.neoforged.neoforge.event.entity.ProjectileImpactEvent e) {
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult eh) || !(eh.getEntity() instanceof ServerPlayer p)) return;
        ArrowParry a = ARROWS.get(p.getUUID());
        if (a == null || p.level().getGameTime() > a.until() || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        var proj = e.getProjectile();
        Vec3 v = proj.getDeltaMovement();
        if (v.lengthSqr() < 1e-4 || p.getLookAngle().dot(v.normalize().reverse()) < 0.25) return;   // tem que estar de frente
        e.setCanceled(true);
        ARROWS.remove(p.getUUID());
        ServerLevel level = p.serverLevel();
        Entity shooter = proj.getOwner();
        double speed = v.length();
        Vec3 nv;
        if (a.perfect() && shooter != null && shooter.isAlive() && shooter.distanceToSqr(p) < 48 * 48) {
            nv = shooter.getEyePosition().add(0, -0.2, 0).subtract(proj.position()).normalize().scale(Math.max(1.6, speed));
            if (proj instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) arrow.setCritArrow(true);
        } else nv = v.scale(-0.45).add((level.random.nextDouble() - 0.5) * 0.3, 0.25, (level.random.nextDouble() - 0.5) * 0.3);
        proj.setDeltaMovement(nv);
        proj.setOwner(p);
        proj.hurtMarked = true;
        Vec3 at = proj.position();
        level.sendParticles(ModParticles.PARRY_SPARK.get(), at.x, at.y, at.z, a.perfect() ? 24 : 12, 0.05, 0.05, 0.05, 0.4);
        level.sendParticles(ModParticles.IMPACT_STAR.get(), at.x, at.y, at.z, 0, a.perfect() ? 1.0 : 0.6, 0, 0, 1.0);
        level.playSound(null, at.x, at.y, at.z, a.perfect() ? ModSounds.PARRY_PERFECT.get() : ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.0f, 1.15f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CombatFxPayload(CombatFxPayload.PARRY, p.getId(), proj.getId(), 0f));
    }
    private static final List<StabHit> STABS = new ArrayList<>();

    // jogador atras do mob (fora do campo de visao dele)
    static boolean behind(Player p, LivingEntity t) {
        double y = Math.toRadians(t.yBodyRot);
        Vec3 face = new Vec3(-Math.sin(y), 0, Math.cos(y));
        Vec3 to = p.position().subtract(t.position()).multiply(1, 0, 1);
        return to.lengthSqr() > 1e-4 && face.dot(to.normalize()) < -0.35;
    }
    private static final List<RushHit> RUSHES = new ArrayList<>();

    public static void onAction(ServerPlayer p, CombatActionPayload m) {
        boolean weapon = CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE;
        boolean shield = p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
        if (!p.isAlive() || !weapon && !(shield && m.kind() == CombatActionPayload.PARRY)) return;
        long now = p.level().getGameTime();
        if (m.kind() == CombatActionPayload.ARROW_PARRY) {   // aparar flecha/projetil: vale por alguns ticks
            if (!weapon) return;
            ARROWS.put(p.getUUID(), new ArrowParry(now + 7 + Telegraphs.grace(p), m.flag()));
            CombatMovesServer.broadcast(p, CombatMove.PARRY, false);
            return;
        }
        Entity ent = p.level().getEntity(m.target());
        if (!(ent instanceof LivingEntity target) || !target.isAlive()) return;
        switch (m.kind()) {
            case CombatActionPayload.PARRY -> {
                if (target.distanceToSqr(p) < 8 * 8) Telegraphs.claim(p, target.getId(), weapon && m.flag());   // escudo: so parry normal
                if (weapon) CombatMovesServer.broadcast(p, CombatMove.PARRY, false);
            }
            case CombatActionPayload.ISSEN -> {
                Window w = ISSEN.remove(p.getUUID());
                CombatMovesServer.broadcast(p, CombatMove.ISSEN, false);
                if (w == null || w.mobId() != target.getId() || now > w.until() || target.distanceToSqr(p) > 6 * 6) return;
                issen(p, target);
            }
            case CombatActionPayload.RUSH -> {
                Window w = RUSH.remove(p.getUUID());
                CombatMovesServer.broadcast(p, CombatMove.RUSH, false);
                if (w == null || w.mobId() != target.getId() || now > w.until() || target.distanceToSqr(p) > 6 * 6) return;
                RUSHES.add(new RushHit(p.getUUID(), target.getId(), now + 1, 5));
                send(p, CombatFxPayload.RUSH, target, 0);
            }
            case CombatActionPayload.BACKSTAB -> {
                CombatMovesServer.broadcast(p, CombatMove.BACKSTAB, false);
                if (!p.isCrouching() || target.distanceToSqr(p) > 3.2 * 3.2 || !behind(p, target) || target instanceof net.minecraft.world.entity.player.Player) return;
                Posture.freeze(target, 24);   // fica parado enquanto a arma entra e sai
                boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
                STABS.add(new StabHit(p.getUUID(), target.getId(), now + 9, axe));
            }
            case CombatActionPayload.CRITICAL -> {
                CombatMovesServer.broadcast(p, CombatMove.CRITICAL, false);
                if (!Posture.broken(target) || target.distanceToSqr(p) > 4.5 * 4.5) return;
                critical(p, target);
            }
            default -> {}
        }
    }

    private static float attack(Player p) { return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE); }

    private static void send(ServerPlayer p, byte kind, LivingEntity mob, float f) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CombatFxPayload(kind, p.getId(), mob.getId(), f));
    }

    private static boolean hit(ServerPlayer p, LivingEntity t, float dmg) {
        t.invulnerableTime = 0;
        CombatMovesServer.ownHit = true;
        boolean ok = t.hurt(p.damageSources().playerAttack(p), dmg);
        CombatMovesServer.ownHit = false;
        return ok;
    }

    // ---------------------------------------------------------------- parry (chamado por Telegraphs)
    static void parried(ServerPlayer p, LivingEntity mob, boolean perfect) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Vec3 mid = p.getEyePosition().add(mob.getEyePosition()).scale(0.5);
        Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? p.getLookAngle() : away.normalize();
        mob.knockback(perfect ? 0.8 : 0.55, -away.x, -away.z);
        Posture.add(mob, Posture.max(mob) * (perfect ? 0.5f : 0.3f));
        level.playSound(null, mid.x, mid.y, mid.z, perfect ? ModSounds.PARRY_PERFECT.get() : ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.1f,
                0.95f + level.random.nextFloat() * 0.1f);
        level.sendParticles(ModParticles.PARRY_SPARK.get(), mid.x, mid.y, mid.z, perfect ? 30 : 16, 0.05, 0.05, 0.05, 0.45);
        level.sendParticles(ModParticles.IMPACT_STAR.get(), mid.x, mid.y, mid.z, 0, perfect ? 1.6 : 1.0, 0, 0, 1.0);
        if (perfect) {
            level.sendParticles(ModParticles.IMPACT_RING.get(), mid.x, mid.y, mid.z, 0, 1.4, 0, 0, 1.0);
            Posture.freeze(mob, 8);
            p.heal(1.0f);
            ISSEN.put(p.getUUID(), new Window(mob.getId(), now + 11 + Telegraphs.grace(p)));
        }
        send(p, CombatFxPayload.PARRY, mob, perfect ? 1 : 0);
    }

    // ---------------------------------------------------------------- esquiva perfeita (chamado por DodgeServer)
    static void perfectDodge(ServerPlayer p, LivingEntity mob) {
        long now = p.level().getGameTime();
        Posture.slow(mob, 30);
        Posture.add(mob, Posture.max(mob) * 0.15f);
        RUSH.put(p.getUUID(), new Window(mob.getId(), now + 30 + Telegraphs.grace(p)));
        send(p, CombatFxPayload.PERFECT_DODGE, mob, 0);
    }

    // ---------------------------------------------------------------- Issen
    private static void issen(ServerPlayer p, LivingEntity t) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = t.position().subtract(p.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? p.getLookAngle().multiply(1, 0, 1) : dir;
        dir = dir.normalize();
        Vec3 from = p.position();
        FINISHED_BY_SPECIAL.put(t.getUUID(), level.getGameTime());
        hit(p, t, attack(p) * 4f);
        Posture.add(t, Posture.max(t));
        // atravessa: para o outro lado do mob, se couber
        Vec3 to = t.position().add(dir.scale(t.getBbWidth() * 0.5 + 1.2));
        Vec3 delta = to.subtract(from);
        if (level.noCollision(p, p.getBoundingBox().move(delta)) && p.hasLineOfSight(t)) p.teleportTo(to.x, p.getY(), to.z);
        Vec3 c = t.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x, c.y, c.z, 0, 2.0, 0, 0, 1.0);
        level.sendParticles(ModParticles.IMPACT_RING.get(), c.x, c.y, c.z, 0, 1.8, 0, 0, 1.0);
        level.sendParticles(ModParticles.PARRY_SPARK.get(), c.x, c.y, c.z, 30, 0.1, 0.2, 0.1, 0.5);
        level.playSound(null, c.x, c.y, c.z, ModSounds.SWORD_HIT.get(), SoundSource.PLAYERS, 1.3f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
        p.getMainHandItem().hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        send(p, CombatFxPayload.ISSEN, t, 0);
    }

    // ---------------------------------------------------------------- golpe critico
    private static void critical(ServerPlayer p, LivingEntity t) {
        ServerLevel level = p.serverLevel();
        FINISHED_BY_SPECIAL.put(t.getUUID(), level.getGameTime());
        if (CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.SWORD) CombatMovesServer.STAB.put(t.getUUID(), level.getGameTime());
        Posture.endBreak(t);
        hit(p, t, attack(p) * 3f);
        Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-4) { away = away.normalize(); t.knockback(0.6, -away.x, -away.z); }
        Vec3 c = t.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x, c.y, c.z, 0, 2.2, 0, 0, 1.0);
        level.sendParticles(ModParticles.IMPACT_RING.get(), c.x, c.y, c.z, 0, 2.0, 0, 0, 1.0);
        level.sendParticles(ModParticles.HIT_SPARK.get(), c.x, c.y, c.z, 40, 0.15, 0.3, 0.15, 0.5);
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        level.playSound(null, c.x, c.y, c.z, axe ? ModSounds.AXE_HIT.get() : ModSounds.SWORD_HIT.get(), SoundSource.PLAYERS, 1.4f, 0.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        p.getMainHandItem().hurtAndBreak(2, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        send(p, CombatFxPayload.CRITICAL, t, 0);
    }

    // ---------------------------------------------------------------- Rajada (5 cortes, um a cada 2 ticks)
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (FINISHED_BY_SPECIAL.size() > 256) FINISHED_BY_SPECIAL.clear();
        // facada pelas costas: o golpe entra no momento certo da animacao
        for (Iterator<StabHit> it = STABS.iterator(); it.hasNext(); ) {
            StabHit s = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(s.player());
            if (p == null) { it.remove(); continue; }
            ServerLevel level = p.serverLevel();
            if (level.getGameTime() < s.at()) continue;
            it.remove();
            if (!(level.getEntity(s.mobId()) instanceof LivingEntity t) || !t.isAlive()) continue;
            FINISHED_BY_SPECIAL.put(t.getUUID(), level.getGameTime());
            if (!s.axe()) CombatMovesServer.STAB.put(t.getUUID(), level.getGameTime());
            hit(p, t, attack(p) * 5f);
            Posture.add(t, Posture.max(t) * 0.6f);
            Vec3 c = t.getBoundingBox().getCenter();
            level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x, c.y, c.z, 0, 1.2, 0, 0, 1.0);
            level.sendParticles(ModParticles.HIT_SPARK.get(), c.x, c.y, c.z, 30, 0.15, 0.25, 0.15, 0.4);
            level.playSound(null, c.x, c.y, c.z, s.axe() ? ModSounds.AXE_HIT.get() : ModSounds.SWORD_HIT.get(), SoundSource.PLAYERS, 1.4f, 0.6f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.7f);
            p.getMainHandItem().hurtAndBreak(2, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        if (RUSHES.isEmpty()) return;
        List<RushHit> next = new ArrayList<>();
        for (Iterator<RushHit> it = RUSHES.iterator(); it.hasNext(); ) {
            RushHit r = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(r.player());
            if (p == null || !p.isAlive()) { it.remove(); continue; }
            ServerLevel level = p.serverLevel();
            if (level.getGameTime() < r.at()) continue;
            it.remove();
            if (!(level.getEntity(r.mobId()) instanceof LivingEntity t) || !t.isAlive() || t.distanceToSqr(p) > 7 * 7) continue;
            if (r.left() == 1) FINISHED_BY_SPECIAL.put(t.getUUID(), level.getGameTime());
            hit(p, t, attack(p) * 0.6f);
            Posture.add(t, Posture.max(t) * 0.08f);
            Vec3 c = t.getBoundingBox().getCenter();
            level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x + (level.random.nextDouble() - 0.5) * 0.6, c.y + (level.random.nextDouble() - 0.5) * 0.6,
                    c.z + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.8, 0, 0, 1.0);
            if (r.left() > 1) next.add(new RushHit(r.player(), r.mobId(), level.getGameTime() + 2, r.left() - 1));
        }
        RUSHES.addAll(next);
    }

    // ---------------------------------------------------------------- recompensa
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        Long t = FINISHED_BY_SPECIAL.remove(v.getUUID());
        if (t == null || !(v.level() instanceof ServerLevel level) || level.getGameTime() - t > 3 || v instanceof Player) return;
        ExperienceOrb.award(level, v.position(), Math.max(2, (int) (v.getMaxHealth() / 3)));
    }
}
