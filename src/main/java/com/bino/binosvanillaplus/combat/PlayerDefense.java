package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Defesa e POSTURA DO JOGADOR (regras de 23/11). O cliente avisa quando segura/solta a tecla Defender; aqui:
//  - bloqueio: golpe de frente (corpo a corpo, flecha, explosao) e' cancelado (100% do dano), a postura enche com
//    1,5x o golpe (metade com escudo) e o cliente paga a estamina (40, ou 20 com escudo).
//  - postura maxima: 100 + peso da armadura (armadura + resistencia) + 3 por ponto de Vitalidade. Esvazia 12/s
//    depois de 1,5 s sem encher.
//  - guarda quebrada (postura cheia ou estamina zerada defendendo): atordoado 1,2 s (lento e fraco) e leva 1,5x de dano.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class PlayerDefense {
    private PlayerDefense() {}

    private record Block(long until, boolean shield) {}
    private static final class Post { float value; long lastAdd; float lastSent = -1; }
    private static final Map<UUID, Block> BLOCKING = new HashMap<>();
    private static final Map<UUID, Post> POSTURE = new HashMap<>();
    private static final Map<UUID, Long> BROKEN = new HashMap<>();

    public static float max(ServerPlayer p) {
        double weight = p.getArmorValue() + p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * 1.5;
        return (float) (100 + weight * 1.5 + 3 * com.bino.binosvanillaplus.leveling.PlayerStats.vitality(p));
    }

    static void onAction(ServerPlayer p, CombatActionPayload m) {
        long now = p.level().getGameTime();
        switch (m.kind()) {
            case CombatActionPayload.BLOCK_START -> {
                if (broken(p)) return;
                BLOCKING.put(p.getUUID(), new Block(now + 40 + Telegraphs.grace(p), m.level() == 1));
            }
            case CombatActionPayload.BLOCK_END -> BLOCKING.remove(p.getUUID());
            case CombatActionPayload.GUARD_BROKEN -> { if (BLOCKING.containsKey(p.getUUID()) || now - post(p).lastAdd < 10) breakGuard(p); }
            default -> {}
        }
    }

    public static boolean broken(ServerPlayer p) {
        Long t = BROKEN.get(p.getUUID());
        return t != null && t > p.level().getGameTime();
    }

    private static Post post(ServerPlayer p) { return POSTURE.computeIfAbsent(p.getUUID(), k -> new Post()); }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();
        if (broken(p)) { e.setAmount(e.getAmount() * 1.5f); return; }   // guarda quebrada: leva mais
        Block b = BLOCKING.get(p.getUUID());
        if (b == null) return;
        if (now > b.until()) { BLOCKING.remove(p.getUUID()); return; }
        var src = e.getSource();
        boolean blockable = src.getEntity() != null || src.getDirectEntity() != null || src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        Vec3 from = src.getSourcePosition();
        if (!blockable || from == null) return;
        Vec3 to = from.subtract(p.getEyePosition()).multiply(1, 0, 1);
        if (to.lengthSqr() > 1e-4 && to.normalize().dot(p.getLookAngle().multiply(1, 0, 1).normalize()) < 0.2) return;   // so de frente
        float amount = e.getAmount();
        e.setCanceled(true);
        Post ps = post(p);
        ps.value += amount * 3f * 1.5f * (b.shield() ? 0.5f : 1f);
        ps.lastAdd = now;
        ServerLevel level = p.serverLevel();
        Vec3 mid = p.getEyePosition().add(p.getLookAngle().scale(0.7)).add(0, -0.3, 0);
        level.sendParticles(ModParticles.HIT_SPARK.get(), mid.x, mid.y, mid.z, 8, 0.05, 0.05, 0.05, 0.3);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), b.shield() ? SoundEvents.SHIELD_BLOCK : SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, b.shield() ? 1f : 0.35f, b.shield() ? 1f : 1.8f);
        PacketDistributor.sendToPlayer(p, new CombatFxPayload(CombatFxPayload.BLOCKED, p.getId(),
                src.getEntity() != null ? src.getEntity().getId() : 0, b.shield() ? 20f : 40f));
        if (ps.value >= max(p)) breakGuard(p);
        else sync(p, ps, false);
    }

    // postura extra no jogador (choque de espadas)
    static void addPosture(ServerPlayer p, float amount) {
        if (broken(p)) return;
        Post ps = post(p);
        ps.value += amount;
        ps.lastAdd = p.level().getGameTime();
        if (ps.value >= max(p)) breakGuard(p);
        else sync(p, ps, false);
    }

    static void breakGuard(ServerPlayer p) {
        long now = p.level().getGameTime();
        BROKEN.put(p.getUUID(), now + 24);
        BLOCKING.remove(p.getUUID());
        Post ps = post(p);
        ps.value = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 3, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 2, false, false, false));
        ServerLevel level = p.serverLevel();
        level.sendParticles(ModParticles.IMPACT_STAR.get(), p.getX(), p.getEyeY() + 0.6, p.getZ(), 0, 1.0, 1, 0, 1.0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
        PacketDistributor.sendToPlayer(p, new CombatFxPayload(CombatFxPayload.PLAYER_POSTURE, p.getId(), 1, 1f));
    }

    private static void sync(ServerPlayer p, Post ps, boolean force) {
        float f = Math.min(1f, ps.value / max(p));
        if (!force && Math.abs(f - ps.lastSent) < 0.02f) return;
        ps.lastSent = f;
        PacketDistributor.sendToPlayer(p, new CombatFxPayload(CombatFxPayload.PLAYER_POSTURE, p.getId(), 0, f));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (POSTURE.isEmpty()) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            Post ps = POSTURE.get(p.getUUID());
            if (ps == null || ps.value <= 0) continue;
            long now = p.level().getGameTime();
            if (now - ps.lastAdd > 30) {
                ps.value = Math.max(0, ps.value - 12f / 20f);
                sync(p, ps, ps.value <= 0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        BLOCKING.remove(id); POSTURE.remove(id); BROKEN.remove(id);
    }
}
