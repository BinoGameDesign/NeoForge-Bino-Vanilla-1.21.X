package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

// KING RAT (Rei Rato, chefe do mod Exploration, regras do Bino em 27/11):
//  - Rato em pe da altura de um golem de ferro (escala 1,45), coroa de ouro. Vida 300, dano 14, armadura 10, sem barra
//    de chefe. Forte o bastante para vencer uns 2 golems de ferro sozinho.
//  - 4 golpes (KingRatAttackGoal):
//      1. Garrada dupla: duas patadas na frente (0,45 s e 0,7 s da animacao), da para aparar (aviso do Combat).
//      2. Cuspe: bola de queijo podre de longe (veneno + nausea) -- KingRatSpitEntity.
//      3. Salto esmagador: pula no jogador e esmaga o chao (dano em volta, joga para cima). Aviso vermelho.
//      4. Guincho do Rei: chama 1 Rato Gigante + 2 ratos contra o jogador que ele esta atacando (a cada 30 s).
//  - Nasce do ninho (KingRatNestBlockEntity) quando um jogador chega a ~10 blocos da cama dele: acorda na cama
//    (animacao "wake", invulneravel). Se o jogador vai embora sem matar, ele some e nasce de novo na proxima visita.
//    Morto, o ninho fica vazio para sempre (outro covil tem outro rei).
//  - Fica perto do covil: sem alvo e longe do ninho, volta para casa.
//  - Ratos e Ratos Gigantes sao aliados dele (isAlliedTo).
public class KingRatEntity extends Monster implements Enemy {
    public static final byte EV_CLAW = 70, EV_SPIT = 71, EV_LEAP = 72, EV_ROAR = 73, EV_WAKE = 74;
    public static final int WAKE_TICKS = 44, DEATH_TICKS = 32;

    public final AnimationState claw = new AnimationState(), spit = new AnimationState(), leap = new AnimationState(),
            roar = new AnimationState(), wake = new AnimationState();

    @Nullable private BlockPos nest;
    private int wakeTicks;
    private final List<UUID> summoned = new ArrayList<>();

    public KingRatEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.SCALE, 1.45);   // ~2,9 blocos: altura de um golem de ferro
    }

    public void setNest(BlockPos pos) { this.nest = pos.immutable(); }
    @Nullable public BlockPos nest() { return nest; }

    public void startWake() { wakeTicks = WAKE_TICKS; event(EV_WAKE); }
    public boolean waking() { return wakeTicks > 0; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WakeGoal(this));
        goalSelector.addGoal(2, new KingRatAttackGoal(this));
        goalSelector.addGoal(3, new HomeGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, RatEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ratos e ratos gigantes sao do lado dele
    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof RatEntity r && !r.isAlly()) return true;
        return super.isAlliedTo(other);
    }

    void event(byte id) { level().broadcastEntityEvent(this, id); }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_CLAW -> claw.start(tickCount);
            case EV_SPIT -> spit.start(tickCount);
            case EV_LEAP -> leap.start(tickCount);
            case EV_ROAR -> roar.start(tickCount);
            case EV_WAKE -> wake.start(tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (wakeTicks > 0) {
            wakeTicks--;
            getNavigation().stop();
            if (wakeTicks == 16) { playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.5f); }
        }
        summoned.removeIf(id -> !(((ServerLevel) level()).getEntity(id) instanceof LivingEntity le) || !le.isAlive());
    }

    // acordando: nao leva dano (a cena de acordar fica inteira)
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (waking() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && nest != null && level().getBlockEntity(nest) instanceof KingRatNestBlockEntity n) n.defeated();
    }

    // a animacao de morte dura mais que o tombo do jogo
    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, net.minecraft.world.entity.EntityEvent.POOF);
            remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------ golpes (chamados pelo KingRatAttackGoal)
    /** garrada: acerta quem esta na frente (arco), a ate 3,6 blocos */
    void clawHit() {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.6),
                t -> t != this && t.isAlive() && !isAlliedTo(t) && !(t instanceof RatEntity r && !r.isAlly()))) {
            Vec3 to = t.position().subtract(position()).multiply(1, 0, 1);
            if (to.length() > 3.6 + t.getBbWidth() / 2 || (to.lengthSqr() > 0.5 && to.normalize().dot(look) < 0.2)) continue;
            if (t.hurt(damageSources().mobAttack(this), dmg * 0.6f)) t.knockback(0.5, -look.x, -look.z);
        }
        playSound(SoundEvents.RAVAGER_ATTACK, 1.0f, 1.3f);
    }

    void spitAt(LivingEntity target) {
        KingRatSpitEntity s = new KingRatSpitEntity(level(), this);
        Vec3 from = getEyePosition().add(Vec3.directionFromRotation(0, getYRot()).scale(0.8));
        s.setPos(from.x, from.y - 0.3, from.z);
        double dx = target.getX() - s.getX(), dy = target.getY(0.4) - s.getY(), dz = target.getZ() - s.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        s.shoot(dx, dy + h * 0.12, dz, 1.3f, 3f);
        level().addFreshEntity(s);
        playSound(SoundEvents.LLAMA_SPIT, 1.4f, 0.6f);
    }

    void leapTowards(LivingEntity target) {
        Vec3 to = target.position().subtract(position());
        Vec3 h = to.multiply(1, 0, 1);
        double d = Math.min(10.0, h.length());
        Vec3 dir = h.lengthSqr() > 1e-4 ? h.normalize() : Vec3.directionFromRotation(0, getYRot());
        setDeltaMovement(dir.x * d * 0.115, 0.72 + Math.max(0, to.y) * 0.08, dir.z * d * 0.115);
        hasImpulse = true;
        playSound(SoundEvents.RAVAGER_STEP, 1.2f, 0.6f);
    }

    void slam() {
        if (!(level() instanceof ServerLevel sl)) return;
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85f;
        for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.5, 1.0, 3.5),
                t -> t != this && t.isAlive() && !isAlliedTo(t) && !(t instanceof RatEntity r && !r.isAlly()))) {
            Vec3 away = t.position().subtract(position()).multiply(1, 0, 1);
            Vec3 n = away.lengthSqr() > 1e-4 ? away.normalize() : new Vec3(1, 0, 0);
            if (t.hurt(damageSources().mobAttack(this), dmg)) {
                t.setDeltaMovement(t.getDeltaMovement().add(n.x * 0.8, 0.55, n.z * 0.8));
                t.hurtMarked = true;
            }
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                getX(), getY() + 0.1, getZ(), 60, 1.6, 0.05, 1.6, 0.2);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 2, 0.6, 0, 0.6, 0);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.7f);
    }

    /** guincho: chama 1 Rato Gigante + 2 ratos contra o alvo (no maximo 6 invocados vivos) */
    void summon(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 1.45f);
        if (summoned.size() >= 6) return;
        for (int i = 0; i < 3; i++) {
            EntityType<? extends RatEntity> type = i == 0 ? ModEntities.DIRE_RAT.get() : ModEntities.RAT.get();
            RatEntity r = type.create(sl);
            if (r == null) continue;
            double a = random.nextDouble() * Math.PI * 2, d = 1.5 + random.nextDouble() * 1.5;
            BlockPos p = BlockPos.containing(getX() + Math.cos(a) * d, getY(), getZ() + Math.sin(a) * d);
            r.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0);
            r.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            sl.addFreshEntity(r);
            sl.sendParticles(ParticleTypes.POOF, r.getX(), r.getY() + 0.3, r.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
            if (target instanceof Player pl) {
                if (r instanceof DireRatEntity) r.setTarget(pl); else r.rally(pl);
            } else r.setTarget(target);
            summoned.add(r.getUUID());
        }
    }

    // ------------------------------------------------------------------ sons
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.HOGLIN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 1.35f + (random.nextFloat() - 0.5f) * 0.1f; }
    @Override public int getAmbientSoundInterval() { return 200; }

    @Override public boolean removeWhenFarAway(double dist) { return false; }

    // ------------------------------------------------------------------ salvar
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (nest != null) tag.put("KingNest", NbtUtils.writeBlockPos(nest));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("KingNest")) nest = NbtUtils.readBlockPos(tag, "KingNest").orElse(null);
    }

    // ------------------------------------------------------------------ objetivos
    // acordando na cama: fica parado olhando o jogador
    static class WakeGoal extends Goal {
        private final KingRatEntity k;
        WakeGoal(KingRatEntity k) { this.k = k; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return k.waking(); }
        @Override public void tick() { k.getNavigation().stop(); }
    }

    // sem alvo e longe do ninho (mais de 16 blocos): volta para os aposentos
    static class HomeGoal extends Goal {
        private final KingRatEntity k;
        HomeGoal(KingRatEntity k) { this.k = k; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return k.nest != null && k.getTarget() == null && k.nest.distToCenterSqr(k.position()) > 16 * 16;
        }
        @Override public boolean canContinueToUse() { return k.getTarget() == null && k.nest.distToCenterSqr(k.position()) > 4 * 4 && !k.getNavigation().isDone(); }
        @Override public void start() { k.getNavigation().moveTo(k.nest.getX() + 0.5, k.nest.getY() + 1, k.nest.getZ() + 0.5, 0.9); }
    }

    // os 4 golpes, um de cada vez, com recarga de cada um
    static class KingRatAttackGoal extends Goal {
        private static final int CLAW = 1, SPIT = 2, LEAP = 3, ROAR = 4;
        private final KingRatEntity k;
        private int move, t;
        private long clawReady, spitReady, leapReady, roarReady;
        private boolean landed;

        KingRatAttackGoal(KingRatEntity k) { this.k = k; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        @Override public boolean canUse() { LivingEntity t = k.getTarget(); return t != null && t.isAlive() && !k.waking(); }
        @Override public boolean canContinueToUse() { return move != 0 || canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { long now = k.level().getGameTime(); roarReady = Math.max(roarReady, now + 100); }
        @Override public void stop() { move = 0; k.getNavigation().stop(); }

        private void begin(int m, byte ev) {
            move = m; t = 0; landed = false;
            k.getNavigation().stop();
            k.event(ev);
        }

        @Override
        public void tick() {
            LivingEntity target = k.getTarget();
            long now = k.level().getGameTime();
            if (target != null) k.getLookControl().setLookAt(target, 30f, 30f);
            if (move != 0) { run(target, now); return; }
            if (target == null) return;
            double d = Math.sqrt(k.distanceToSqr(target));
            boolean see = k.getSensing().hasLineOfSight(target);
            float hp = k.getHealth() / k.getMaxHealth();
            if (now >= roarReady && see && (hp < 0.75f || k.random.nextFloat() < 0.01f)) { begin(ROAR, EV_ROAR); roarReady = now + 600; return; }
            if (d < 3.6 && now >= clawReady) { begin(CLAW, EV_CLAW); clawReady = now + 30; warn(target, 9, false); return; }
            if (d > 5 && d < 12 && k.onGround() && now >= leapReady && see && k.random.nextFloat() < 0.05f) {
                begin(LEAP, EV_LEAP); leapReady = now + 140; warn(target, 17, true); return;
            }
            if (d > 6 && d < 16 && now >= spitReady && see && k.random.nextFloat() < 0.06f) { begin(SPIT, EV_SPIT); spitReady = now + 90; return; }
            if (k.getNavigation().isDone() || k.tickCount % 10 == 0) k.getNavigation().moveTo(target, d > 8 ? 1.15 : 1.0);
        }

        // aviso do mod Combat (dá para aparar a garrada; o salto e' vermelho, so desviando)
        private void warn(LivingEntity target, int windup, boolean red) {
            if (target instanceof ServerPlayer sp) com.bino.binoscombat.combat.Telegraphs.start(k, sp, windup, red);
        }

        private void run(LivingEntity target, long now) {
            t++;
            k.getNavigation().stop();
            switch (move) {
                case CLAW -> {   // 20 ticks; patadas em 9 e 14 (marcadores da animacao: 0,45 s e 0,7 s)
                    if (t == 9 || t == 14) k.clawHit();
                    if (t >= 20) move = 0;
                }
                case SPIT -> {   // 20 ticks; cospe em 10 (0,5 s)
                    if (t == 10 && target != null) k.spitAt(target);
                    if (t >= 20) move = 0;
                }
                case LEAP -> {   // 26 ticks; salta em 7, esmaga quando toca o chao
                    if (t == 7 && target != null) k.leapTowards(target);
                    if (t > 9 && !landed && k.onGround()) { landed = true; k.slam(); }
                    if (t >= 26) { if (!landed) k.slam(); move = 0; }
                }
                case ROAR -> {   // 32 ticks; chama os ratos em 10
                    if (t == 10 && target != null) k.summon(target);
                    if (t >= 32) move = 0;
                }
                default -> move = 0;
            }
        }
    }
}
