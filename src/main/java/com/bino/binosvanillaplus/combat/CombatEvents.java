package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Mod COMBAT (lado do servidor): sons de espada/machado, faiscas na direcao do golpe e numeros de dano.
// O lado do cliente (flash branco, recuo do alvo, tremida/travada da camera, desenho dos numeros) fica em
// client/combat.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class CombatEvents {
    private CombatEvents() {}

    // tipos de numero (cor e texto no cliente)
    public static final byte KIND_PHYSICAL = 0, KIND_FIRE = 1, KIND_CRIT = 2, KIND_LIGHTNING = 3, KIND_DROWN = 4,
            KIND_POISON = 5, KIND_BLOCK = 6, KIND_MISS = 7;

    public enum Weapon { NONE, SWORD, AXE }

    public static Weapon weapon(ItemStack s) {
        if (s.getItem() instanceof SwordItem || s.is(ItemTags.SWORDS)) return Weapon.SWORD;
        if (s.getItem() instanceof AxeItem || s.is(ItemTags.AXES)) return Weapon.AXE;
        return Weapon.NONE;
    }

    // ---------------------------------------------------------------- critico (marca o alvo ate o dano chegar)
    private static final Set<UUID> CRIT = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCrit(CriticalHitEvent e) {
        if (!e.getEntity().level().isClientSide && e.isCriticalHit()) CRIT.add(e.getTarget().getUUID());
    }

    // ---------------------------------------------------------------- dano: som, faiscas, numero
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        DamageSource src = e.getSource();
        boolean crit = CRIT.remove(victim.getUUID());
        Entity atk = src.getEntity();
        boolean melee = atk instanceof LivingEntity && src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE);

        if (melee) {
            LivingEntity a = (LivingEntity) atk;
            Weapon w = weapon(a.getMainHandItem());
            if (w != Weapon.NONE) {
                SoundEvent s = w == Weapon.SWORD ? ModSounds.SWORD_HIT.get() : ModSounds.AXE_HIT.get();
                level.playSound(null, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), s, SoundSource.PLAYERS,
                        1.0f, 0.92f + level.random.nextFloat() * 0.16f);
            }
            sparks(level, a, victim, w == Weapon.NONE ? 4 : crit ? 14 : 9);
        }
        send(victim, e.getNewDamage(), crit ? KIND_CRIT : kind(src, victim));
    }

    static byte kind(DamageSource src, LivingEntity victim) {
        if (src.is(DamageTypeTags.IS_LIGHTNING)) return KIND_LIGHTNING;
        if (src.is(DamageTypeTags.IS_FIRE)) return KIND_FIRE;
        if (src.is(DamageTypeTags.IS_DROWNING)) return KIND_DROWN;
        if (src.is(DamageTypes.MAGIC) && victim.hasEffect(MobEffects.POISON)) return KIND_POISON;
        return KIND_PHYSICAL;
    }

    // faiscas voando na direcao do golpe, saindo do ponto de contato
    static void sparks(ServerLevel level, LivingEntity attacker, LivingEntity victim, int count) {
        Vec3 dir = victim.position().subtract(attacker.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1.0e-4 ? attacker.getLookAngle() : dir.normalize();
        Vec3 look = attacker.getLookAngle();
        dir = dir.add(0, look.y * 0.5 + 0.15, 0).normalize();
        Vec3 hit = victim.position().add(0, victim.getBbHeight() * 0.6, 0).subtract(dir.scale(victim.getBbWidth() * 0.45));
        for (int i = 0; i < count; i++) {
            Vec3 v = dir.add((level.random.nextDouble() - 0.5) * 0.9, (level.random.nextDouble() - 0.3) * 0.7, (level.random.nextDouble() - 0.5) * 0.9).normalize();
            double sp = 0.25 + level.random.nextDouble() * 0.35;
            level.sendParticles(ModParticles.HIT_SPARK.get(), hit.x, hit.y, hit.z, 0, v.x, v.y, v.z, sp);
        }
    }

    public static void send(LivingEntity victim, float amount, byte kind) {
        if (amount <= 0 && kind != KIND_BLOCK && kind != KIND_MISS) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim, new DamageNumberPayload(victim.getId(), amount, kind));
    }

    // escudo segurou o golpe: "Block"
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlock(LivingShieldBlockEvent e) {
        if (!e.getEntity().level().isClientSide && e.getBlocked()) send(e.getEntity(), 0, KIND_BLOCK);
    }

    // esquiva (armadura de rubi): "Miss" (chamado por quem cancela o dano)
    public static void miss(LivingEntity e) { send(e, 0, KIND_MISS); }

    // ---------------------------------------------------------------- golpe em bloco (coisa dura)
    @SubscribeEvent
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel level) || e.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        Weapon w = weapon(e.getItemStack());
        if (w == Weapon.NONE) return;
        BlockPos pos = e.getPos();
        Direction face = e.getFace() != null ? e.getFace() : Direction.UP;
        Vec3 p = Vec3.atCenterOf(pos).add(face.getStepX() * 0.52, face.getStepY() * 0.52, face.getStepZ() * 0.52);
        level.playSound(null, p.x, p.y, p.z, w == Weapon.SWORD ? ModSounds.SWORD_HIT_HARD.get() : ModSounds.AXE_HIT_HARD.get(),
                SoundSource.PLAYERS, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
        Vec3 out = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        for (int i = 0; i < 6; i++) {
            Vec3 v = out.add((level.random.nextDouble() - 0.5) * 1.2, level.random.nextDouble() * 0.6, (level.random.nextDouble() - 0.5) * 1.2).normalize();
            level.sendParticles(ModParticles.HIT_SPARK.get(), p.x, p.y, p.z, 0, v.x, v.y, v.z, 0.22 + level.random.nextDouble() * 0.2);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)), p.x, p.y, p.z, 5, 0.1, 0.1, 0.1, 0.05);
    }

    // ---------------------------------------------------------------- golpe no ar / na agua (o cliente avisa)
    public static void onSwing(ServerPlayer p, SwingPayload m) {
        Weapon w = weapon(p.getMainHandItem());
        if (w == Weapon.NONE) return;
        ServerLevel level = p.serverLevel();
        if (m.water() && p.distanceToSqr(m.x(), m.y(), m.z()) < 36) {
            level.playSound(p, m.x(), m.y(), m.z(), ModSounds.HIT_WATER.get(), SoundSource.PLAYERS, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.SPLASH, m.x(), m.y(), m.z(), 14, 0.25, 0.05, 0.25, 0.2);
        } else {
            level.playSound(p, p.getX(), p.getEyeY(), p.getZ(), w == Weapon.SWORD ? ModSounds.SWORD_SWING.get() : ModSounds.AXE_SWING.get(),
                    SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
        }
    }

}
