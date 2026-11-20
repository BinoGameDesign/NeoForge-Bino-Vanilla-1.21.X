package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

// Rato (mod Exploration): pequeno, medroso, vive nas cavernas. Variante "deep" abaixo de DEEP_Y.
// Agressivo so em enxame (evento raro) ou perto de um dire_rat (mob futuro) -- e mesmo assim foge de gatos e jaguatiricas.
public class RatEntity extends PathfinderMob {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final int DEEP_Y = -32;               // abaixo disso nasce o rato das profundezas
    public static final float SWARM_CHANCE = 0.01f;     // 1% dos ratos que nascem sozinhos chegam em enxame
    public static final int SWARM_SIZE = 10;
    public static final int SWARM_TICKS = 20 * 120;     // o enxame fica bravo por 2 min
    public static final int POISON_ONE_IN = 12;         // 1 em 12 mordidas envenena
    public static final int POISON_TICKS = 100;         // 5 s de veneno

    public static final int VAR_COMMON = 0, VAR_DEEP = 1;
    public static final int ST_NORMAL = 0, ST_FLEE = 1, ST_SLEEP = 2, ST_EAT = 3;
    public static final byte EV_GROOM = 70, EV_STAND = 71, EV_SCRATCH = 72, EV_SNIFF = 73, EV_SQUEAK = 74, EV_ATTACK = 75, EV_TAIL_WHIP = 76;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(RatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(RatEntity.class, EntityDataSerializers.INT);

    // cliente
    public final AnimationState oneShot = new AnimationState();
    public String oneShotName = "";
    private int oneShotEnd, blinkIn = 30, blinkEnd;

    // servidor
    private int swarmTicks, sleepTicks, eatTicks, wantSleep;
    int rallyTicks;   // chamado por um Rato Gigante para atacar um jogador

    public RatEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.SCALE, 0.85);   // um pouco menor que o modelo
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, VAR_COMMON);
        b.define(STATE, ST_NORMAL);
    }

    public int getVariant() { return entityData.get(VARIANT); }
    public boolean isDeep() { return getVariant() == VAR_DEEP; }
    public int getState() { return entityData.get(STATE); }
    void setState(int s) { if (getState() != s) entityData.set(STATE, s); }

    // bravo: em enxame ou chamado por um Rato Gigante
    public boolean isAngry() { return swarmTicks > 0 || rallyTicks > 0; }

    // um Rato Gigante agrou esse jogador: os ratos por perto vem junto
    public static final int RALLY_TICKS = 20 * 30;
    public void rally(Player p) {
        rallyTicks = RALLY_TICKS;
        if (getTarget() != p) setTarget(p);
        if (getState() == ST_SLEEP || getState() == ST_EAT) setState(ST_NORMAL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.TemptGoal(this, 1.0,
                net.minecraft.world.item.crafting.Ingredient.of(com.bino.binosvanillaplus.item.ModItems.CHEESE_SLICE.get(), com.bino.binosvanillaplus.block.ModBlocks.CHEESE.get()), false));   // queijo na mao atrai
        goalSelector.addGoal(1, new FleeGoal(this));
        goalSelector.addGoal(2, new SleepGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.addGoal(3, new BaitGoal(this));
        goalSelector.addGoal(4, new EatCakeGoal(this, st -> st.is(RatSpawning.RAT_FOOD)));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> isAngry()));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                m -> isAngry() && !(m instanceof RatEntity) && !(m instanceof Cat) && !(m instanceof Ocelot)));
    }

    // de quem foge: gatos e jaguatiricas sempre; o resto so quando nao esta bravo
    boolean scares(LivingEntity e) {
        if (e instanceof RatEntity) return false;
        if (e instanceof Cat || e instanceof Ocelot) return true;
        if (isAngry()) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof Mob;
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) { clientTick(); return; }
        if (alarmTicks > 0) alarmTicks--;
        if (swarmTicks > 0 && --swarmTicks == 0 && rallyTicks == 0) setTarget(null);
        if (rallyTicks > 0 && --rallyTicks == 0 && swarmTicks == 0) setTarget(null);
        if (eatTicks > 0 && --eatTicks == 0 && getState() == ST_EAT) setState(ST_NORMAL);
        if (getState() == ST_NORMAL && getNavigation().isDone() && getTarget() == null) {
            if (random.nextInt(350) == 0) {
                byte[] evs = {EV_GROOM, EV_STAND, EV_SCRATCH, EV_SNIFF, EV_SQUEAK};
                byte ev = evs[random.nextInt(evs.length)];
                event(ev);
                if (ev == EV_SQUEAK) playSound(ModSounds.RAT_SQUEAK.get(), 0.6f, 1.0f);
            }
            if (random.nextInt(1200) == 0) wantSleep = 1;
        }
    }

    void event(byte id) { level().broadcastEntityEvent(this, id); }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            event(EV_ATTACK);
            playSound(ModSounds.RAT_ATTACK.get(), 0.7f, 1.0f);
            if (target instanceof LivingEntity le && random.nextInt(POISON_ONE_IN) == 0)
                le.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), this);
        }
        return ok;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok && !level().isClientSide) {
            sleepTicks = 0; wantSleep = 0;
            if (getState() == ST_SLEEP || getState() == ST_EAT) setState(ST_NORMAL);
        }
        return ok;
    }

    // ------------------------------------------------------------------ cliente
    public boolean eyesClosed() { return getState() == ST_SLEEP || tickCount < blinkEnd; }

    @Override
    public void handleEntityEvent(byte id) {
        String a = switch (id) {
            case EV_GROOM -> "groom"; case EV_STAND -> "stand_up"; case EV_SCRATCH -> "scratch";
            case EV_SNIFF -> "sniff"; case EV_SQUEAK -> "squeak"; case EV_ATTACK -> "attack"; case EV_TAIL_WHIP -> "tail_whip";
            default -> null;
        };
        if (a == null) { super.handleEntityEvent(id); return; }
        oneShotName = a;
        oneShotEnd = tickCount + oneShotLength(a);
        oneShot.start(tickCount);
    }

    // duracao em ticks; tem que bater com o rat.bbmodel (RatAnimations.lengthTicks)
    protected int oneShotLength(String a) {
        return switch (a) {
            case "groom" -> 64; case "stand_up" -> 68; case "scratch" -> 32;
            case "sniff" -> 40; case "squeak" -> 10; case "attack" -> 9;
            default -> 20;
        };
    }

    private void clientTick() {
        if (oneShot.isStarted() && (tickCount >= oneShotEnd || getState() == ST_SLEEP)) oneShot.stop();
        if (--blinkIn <= 0) { blinkEnd = tickCount + 3; blinkIn = random.nextFloat() < 0.2f ? 6 : 40 + random.nextInt(100); }
        if (getState() == ST_SLEEP && tickCount % 25 == 0) {   // ZzzZZ
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            level().addParticle(ModParticles.SLEEP_Z.get(), getX() + look.x * 0.3, getY() + 0.45, getZ() + look.z * 0.3, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ nome, sons
    @Override
    protected Component getTypeName() {
        return isDeep() ? Component.translatable("entity.binosvanillaplus.deep_rat") : super.getTypeName();
    }

    @Override @Nullable protected SoundEvent getAmbientSound() { return getState() == ST_SLEEP ? null : ModSounds.RAT_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.RAT_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.RAT_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.5f; }
    @Override public int getAmbientSoundInterval() { return 240; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.SILVERFISH_STEP, 0.08f, 1.6f);
    }

    // ------------------------------------------------------------------ nascer / salvar
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        entityData.set(VARIANT, blockPosition().getY() < DEEP_Y ? VAR_DEEP : VAR_COMMON);
        if (canSwarm() && type == MobSpawnType.NATURAL && level.getRandom().nextFloat() < SWARM_CHANCE) startSwarm(level);
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    // so o rato comum chega em enxame (o Rato Gigante nao)
    protected boolean canSwarm() { return true; }

    // evento raro: ~10 ratos juntos, bravos, atacam jogadores e mobs
    private void startSwarm(ServerLevelAccessor level) {
        swarmTicks = SWARM_TICKS;
        for (int i = 1; i < SWARM_SIZE; i++) {
            RatEntity r = ModEntities.RAT.get().create(level.getLevel());
            if (r == null) continue;
            r.moveTo(getX() + (random.nextDouble() - 0.5) * 4, getY(), getZ() + (random.nextDouble() - 0.5) * 4, random.nextFloat() * 360, 0);
            r.finalizeSpawn(level, level.getCurrentDifficultyAt(r.blockPosition()), MobSpawnType.EVENT, null);
            r.swarmTicks = SWARM_TICKS;
            level.addFreshEntity(r);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RatVariant", getVariant());
        tag.putInt("RatSwarm", swarmTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getInt("RatVariant"));
        swarmTicks = tag.getInt("RatSwarm");
    }

    // ================================================================== OBJETIVOS

    // Foge correndo (estado FLEE escolhe a animacao de correr) para o ESCURO e para fora da vista de quem assustou.
    // Quando um rato se assusta, os ratos calmos por perto tambem fogem (alarme do grupo).
    static class FleeGoal extends AvoidEntityGoal<LivingEntity> {
        private final RatEntity r;
        FleeGoal(RatEntity r) { super(r, LivingEntity.class, 6.0f, 1.2, 1.6, r::scares); this.r = r; }
        @Override public boolean canUse() {
            boolean own = super.canUse();
            if (!own) {   // nao viu nada, mas um vizinho deu o alarme
                LivingEntity a = r.alarmTicks > 0 ? r.alarmFrom() : null;
                if (a == null) return false;
                toAvoid = a;
            }
            Vec3 spot = com.bino.binosvanillaplus.entity.ai.StealthAI.fleeSpot(r, toAvoid.position(), toAvoid, 12);
            if (spot != null) {
                var p = pathNav.createPath(spot.x, spot.y, spot.z, 0);
                if (p != null) path = p;
            }
            return path != null;
        }
        @Override public void start() {
            super.start();
            r.setState(ST_FLEE);
            if (r.alarmTicks <= 0 && toAvoid != null)   // so quem viu da o alarme (nao fica em cadeia infinita)
                for (RatEntity o : r.level().getEntitiesOfClass(RatEntity.class, r.getBoundingBox().inflate(8),
                        o -> o != r && !o.isAngry() && o.alarmTicks <= 0))
                    o.alarm(toAvoid);
        }
        @Override public void stop() { super.stop(); if (r.getState() == ST_FLEE) r.setState(ST_NORMAL); }
    }

    // alarme do grupo: foge de 'e' por alguns segundos mesmo sem ter visto
    int alarmTicks;
    @Nullable private java.util.UUID alarmId;
    void alarm(LivingEntity e) {
        alarmTicks = 60;
        alarmId = e.getUUID();
        if (getState() == ST_SLEEP || getState() == ST_EAT) setState(ST_NORMAL);
    }
    @Nullable LivingEntity alarmFrom() {
        if (alarmId == null || !(level() instanceof net.minecraft.server.level.ServerLevel sl)) return null;
        return sl.getEntity(alarmId) instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    // ao passear, prefere cantos escuros
    @Override
    public float getWalkTargetValue(BlockPos pos, net.minecraft.world.level.LevelReader level) {
        return -level.getMaxLocalRawBrightness(pos);
    }

    // cochilo: quando esta calmo e sem ninguem por perto; acorda com barulho de gente chegando ou dano
    static class SleepGoal extends Goal {
        private final RatEntity r;
        SleepGoal(RatEntity r) { this.r = r; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        private boolean calm() {
            return r.getTarget() == null && !r.isAngry() && r.level().getNearestPlayer(r, 6.0) == null;
        }
        @Override public boolean canUse() { return r.wantSleep > 0 && r.getState() == ST_NORMAL && calm(); }
        @Override public boolean canContinueToUse() { return r.sleepTicks > 0 && calm(); }
        @Override public void start() {
            r.wantSleep = 0;
            r.sleepTicks = 600 + r.getRandom().nextInt(1200);
            r.getNavigation().stop();
            r.setState(ST_SLEEP);
        }
        @Override public void tick() { r.sleepTicks--; r.getNavigation().stop(); }
        @Override public void stop() { r.sleepTicks = 0; if (r.getState() == ST_SLEEP) r.setState(ST_NORMAL); }
    }

    // bolo por perto: vai la e come um pedaco (o ultimo pedaco some)
    static class EatCakeGoal extends Goal {
        private final RatEntity r;
        @Nullable private BlockPos cake;
        private int time, chew;
        private final java.util.function.Predicate<BlockState> food;
        EatCakeGoal(RatEntity r, java.util.function.Predicate<BlockState> food) { this.r = r; this.food = food; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (r.getState() != ST_NORMAL || r.getRandom().nextInt(60) != 0) return false;
            BlockPos base = r.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, -2, -8), base.offset(8, 2, 8))) {
                if (food.test(r.level().getBlockState(p))) { cake = p.immutable(); return true; }
            }
            return false;
        }
        @Override public boolean canContinueToUse() {
            return cake != null && time < 400 && food.test(r.level().getBlockState(cake)) && r.getState() != ST_FLEE;
        }
        @Override public void start() { time = 0; chew = 0; r.getNavigation().moveTo(cake.getX() + 0.5, cake.getY(), cake.getZ() + 0.5, 1.0); }
        @Override public void stop() { cake = null; if (r.getState() == ST_EAT) r.setState(ST_NORMAL); }
        @Override public void tick() {
            if (cake == null) return;
            time++;
            r.getLookControl().setLookAt(cake.getX() + 0.5, cake.getY() + 0.3, cake.getZ() + 0.5);
            if (r.distanceToSqr(Vec3.atCenterOf(cake)) > 2.0) {
                if (time % 20 == 0) r.getNavigation().moveTo(cake.getX() + 0.5, cake.getY(), cake.getZ() + 0.5, 1.0);
                return;
            }
            r.getNavigation().stop();
            if (chew == 0) { r.setState(ST_EAT); r.eatTicks = 0; }
            if (++chew >= 60) {   // terminou de comer: tira um pedaco (o objetivo acaba e o rato vai embora)
                BlockState s = r.level().getBlockState(cake);
                if (s.getBlock() instanceof com.bino.binosvanillaplus.block.custom.CheeseBlock) {
                    com.bino.binosvanillaplus.block.custom.CheeseBlock.takeBite(r.level(), cake, s);
                } else if (s.getBlock() instanceof CakeBlock) {
                    int bites = s.getValue(CakeBlock.BITES);
                    if (bites < 6) r.level().setBlock(cake, s.setValue(CakeBlock.BITES, bites + 1), 3);
                    else r.level().removeBlock(cake, false);
                } else if (s.getBlock() instanceof net.minecraft.world.level.block.CandleCakeBlock) {   // derruba a vela e morde
                    net.minecraft.world.level.block.Block.dropResources(s, r.level(), cake);
                    r.level().setBlock(cake, net.minecraft.world.level.block.Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES, 1), 3);
                } else r.level().removeBlock(cake, false);   // outra comida da tag: some
                r.playSound(ModSounds.RAT_EAT.get(), 0.6f, 1.0f);
                r.setState(ST_NORMAL);
                cake = null;
            }
        }
    }

    // ratoeira com isca a ate 16 blocos: vai ate ela (e pisa)
    static class BaitGoal extends Goal {
        private final RatEntity r;
        @Nullable private BlockPos trap;
        private int time;
        BaitGoal(RatEntity r) { this.r = r; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (r.isAngry() || r.getState() == ST_SLEEP || r.getRandom().nextInt(20) != 0) return false;
            double best = 16 * 16; trap = null;
            for (BlockPos p : com.bino.binosvanillaplus.block.entity.RatTrapBlockEntity.baited(r.level())) {
                double d = p.distToCenterSqr(r.position());
                if (d < best) { best = d; trap = p; }
            }
            return trap != null;
        }
        @Override public boolean canContinueToUse() {
            return trap != null && time < 400 && com.bino.binosvanillaplus.block.entity.RatTrapBlockEntity.baited(r.level()).contains(trap) && r.getState() != ST_FLEE;
        }
        @Override public void start() { time = 0; r.getNavigation().moveTo(trap.getX() + 0.5, trap.getY(), trap.getZ() + 0.5, 1.0); }
        @Override public void stop() { trap = null; }
        @Override public void tick() {
            time++;
            if (trap != null && time % 20 == 0) r.getNavigation().moveTo(trap.getX() + 0.5, trap.getY(), trap.getZ() + 0.5, 1.0);
        }
    }

    // usado pelo Cave Goblin: rato calmo e presa; rato de enxame assusta
    public static boolean isEasyPrey(LivingEntity e) { return e instanceof RatEntity r && !(r instanceof DireRatEntity) && !r.isAngry(); }
}
