package com.bino.binosvanillaplus.entity.goblin;

import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

// Cave Goblin (mod Exploration): pequeno, medroso, vive no escuro das cavernas fundas e furta itens do inventario
// de quem esta de costas. Estados sincronizados com o cliente escolhem a animacao (ver CaveGoblinModel).
public class CaveGoblinEntity extends PathfinderMob {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final float RING_CHANCE = 0.005f;      // chance de nascer com o anel (e de dropar ele): 0,5%
    public static final int SACK_SIZE = 6;               // quantos montes roubados cabem no saco
    public static final int STEAL_COOLDOWN = 2400;       // 2 min entre furtos
    public static final int FLEE_AFTER_STEAL = 400;      // foge por 20 s depois de furtar
    public static final double VIEW_CONE = 0.34;         // cos(70 graus): dentro disso o jogador "esta olhando"
    public static final int DARK_LIMIT = 8;              // luz de bloco a partir da qual ele procura o escuro
    public static final int RING_PLAYERS = 2;            // com tantos jogadores por perto ele usa o anel

    public static final int ST_NORMAL = 0, ST_STALK = 1, ST_FLEE = 2, ST_COWER = 3, ST_SHIELD = 4, ST_EAT = 5;
    public static final byte EV_STEAL = 60, EV_GIGGLE = 61, EV_STARTLED = 62, EV_RING = 63, EV_LOOK = 64, EV_FIDGET = 65, EV_SNIFF = 66;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CaveGoblinEntity.class, EntityDataSerializers.INT);

    // ---- cliente: estados de animacao
    public final AnimationState idleState = new AnimationState();
    public final AnimationState lurkState = new AnimationState();
    public final AnimationState cowerState = new AnimationState();
    public final AnimationState shieldState = new AnimationState();
    public final AnimationState eatState = new AnimationState();
    public final AnimationState oneShot = new AnimationState();
    public String oneShotName = "";
    private int oneShotEnd;
    private int blinkIn = 40, blinkEnd;   // piscada por textura (cave_goblin_blink.png), igual ao Fresh Animations

    // ---- servidor
    private final NonNullList<ItemStack> sack = NonNullList.withSize(SACK_SIZE, ItemStack.EMPTY);
    private boolean hasRing;
    private boolean ringActive;
    @Nullable private UUID fleeFrom;
    private int fleeTicks;
    private int stealCooldown;
    private int giggleIn;
    private int eatTicks, huntCooldown;
    private StealGoal stealGoal;

    public CaveGoblinEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);   // so usa para cacar ratos
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, ST_NORMAL);
    }

    public int getState() { return entityData.get(STATE); }
    private void setState(int s) { if (getState() != s) entityData.set(STATE, s); }

    @Override
    protected void registerGoals() {
        stealGoal = new StealGoal(this);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0f, 1.0, 1.4, this::isScary));
        goalSelector.addGoal(3, stealGoal);
        goalSelector.addGoal(4, new EatMeatGoal(this));
        goalSelector.addGoal(4, new HuntRatGoal(this));
        goalSelector.addGoal(4, new SeekDarknessGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // de quem ele tem medo: jogadores (menos o que ele esta espreitando) e qualquer outro mob que nao seja presa nem goblin
    private boolean isScary(LivingEntity e) {
        if (e instanceof CaveGoblinEntity || e instanceof Bat || isPrey(e)) return false;
        if (e instanceof Player p) return !(stealGoal != null && stealGoal.target == p);
        return e instanceof Mob;
    }

    // presas: ratos calmos (rato de enxame assusta)
    public static boolean isPrey(LivingEntity e) { return com.bino.binosvanillaplus.entity.rat.RatEntity.isEasyPrey(e); }

    // ------------------------------------------------------------------ escuro
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return -level.getMaxLocalRawBrightness(pos);   // ao passear, prefere o escuro
    }

    private int blockLight() { return level().getBrightness(LightLayer.BLOCK, blockPosition()); }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) { clientAnimations(); return; }
        if (fleeTicks > 0) fleeTicks--;
        if (stealCooldown > 0) stealCooldown--;
        if (huntCooldown > 0) huntCooldown--;
        if (eatTicks > 0) { getNavigation().stop(); if (--eatTicks == 0 && getState() == ST_EAT) setState(ST_NORMAL); }
        if (giggleIn > 0 && --giggleIn == 0) { event(EV_GIGGLE); playSound(ModSounds.CAVE_GOBLIN_GIGGLE.get(), 1.0f, 1.0f); }
        if (ringActive && !hasEffect(MobEffects.INVISIBILITY)) ringActive = false;
        // o anel: com varios jogadores por perto ele fica invisivel para furtar
        if (hasRing && !ringActive && getState() == ST_STALK && tickCount % 20 == 0
                && level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(16), p -> !p.isSpectator() && !p.isCreative()).size() >= RING_PLAYERS) {
            ringActive = true;
            event(EV_RING);
            playSound(ModSounds.CAVE_GOBLIN_RING.get(), 1.0f, 1.0f);
            addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 60 * 5, 0, false, false));
        }
        // manias quando esta parado e tranquilo
        if (getState() == ST_NORMAL && getNavigation().isDone() && random.nextInt(500) == 0) {
            byte[] evs = {EV_LOOK, EV_FIDGET, EV_SNIFF};
            event(evs[random.nextInt(evs.length)]);
        }
        if (!sackEmpty() || hasRing) setPersistenceRequired();
    }

    private void event(byte id) { level().broadcastEntityEvent(this, id); }

    @Override
    public void handleEntityEvent(byte id) {
        String anim = switch (id) {
            case EV_STEAL -> "steal";
            case EV_GIGGLE -> "giggle";
            case EV_STARTLED -> "startled";
            case EV_RING -> "ring_on";
            case EV_LOOK -> "look_around";
            case EV_FIDGET -> "idle_fidget";
            case EV_SNIFF -> "sniff";
            default -> null;
        };
        if (anim == null) { super.handleEntityEvent(id); return; }
        oneShotName = anim;
        oneShotEnd = tickCount + oneShotLength(anim);
        oneShot.start(tickCount);
    }

    // duracao (ticks) das animacoes de uma vez so. Tem que bater com o .bbmodel (CaveGoblinAnimations.lengthTicks).
    private static int oneShotLength(String anim) {
        return switch (anim) {
            case "steal" -> 20;
            case "giggle" -> 24;
            case "startled" -> 16;
            case "ring_on" -> 32;
            case "look_around" -> 52;
            case "idle_fidget" -> 56;
            case "sniff" -> 24;
            default -> 20;
        };
    }

    // olhos fechados agora? (o renderer troca a textura)
    public boolean eyesClosed() { return tickCount < blinkEnd || getState() == ST_COWER; }

    private void clientAnimations() {
        if (--blinkIn <= 0) {
            blinkEnd = tickCount + 3;                                        // 3 ticks de olho fechado
            blinkIn = random.nextFloat() < 0.2f ? 6 : 50 + random.nextInt(110); // as vezes pisca duas vezes
        }
        if (oneShot.isStarted() && tickCount >= oneShotEnd) oneShot.stop();
        int st = getState();
        boolean moving = walkAnimation.isMoving();
        idleState.animateWhen(st == ST_NORMAL && !moving, tickCount);
        lurkState.animateWhen(st == ST_STALK && !moving, tickCount);
        cowerState.animateWhen(st == ST_COWER, tickCount);
        shieldState.animateWhen(st == ST_SHIELD, tickCount);
        eatState.animateWhen(st == ST_EAT, tickCount);
    }

    // ------------------------------------------------------------------ dano / susto
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok && !level().isClientSide) {
            Entity src = source.getEntity();
            if (src != null) runFrom(src, 200);
            event(EV_STARTLED);
        }
        return ok;
    }

    public void runFrom(Entity threat, int ticks) {
        fleeFrom = threat.getUUID();
        fleeTicks = Math.max(fleeTicks, ticks);
    }

    @Nullable
    Entity threat() {
        if (fleeFrom == null || !(level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(fleeFrom);
        return e != null && e.isAlive() ? e : null;
    }

    // ------------------------------------------------------------------ furto
    static boolean canBeRobbed(Player p) {
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) return false;
        Inventory inv = p.getInventory();
        for (int i = 9; i < 36; i++) if (!inv.getItem(i).isEmpty()) return true;   // so o inventario principal (sem hotbar, armadura, mao secundaria)
        return false;
    }

    // o jogador esta olhando pra ele?
    boolean isSeenBy(Player p) {
        if (isInvisible()) return false;
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        if (to.lengthSqr() < 1.0e-4) return true;
        return p.getViewVector(1.0f).normalize().dot(to.normalize()) > VIEW_CONE && p.hasLineOfSight(this);
    }

    boolean sackEmpty() { for (ItemStack s : sack) if (!s.isEmpty()) return false; return true; }
    boolean sackFull() { for (ItemStack s : sack) if (s.isEmpty()) return false; return true; }

    void steal(Player p) {
        Inventory inv = p.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 9; i < 36; i++) if (!inv.getItem(i).isEmpty()) slots.add(i);
        if (slots.isEmpty() || sackFull()) return;
        int slot = slots.get(random.nextInt(slots.size()));
        ItemStack taken = inv.removeItemNoUpdate(slot);
        inv.setChanged();
        for (int i = 0; i < sack.size(); i++) if (sack.get(i).isEmpty()) { sack.set(i, taken); break; }

        event(EV_STEAL);
        playSound(ModSounds.CAVE_GOBLIN_STEAL.get(), 1.0f, 1.0f);
        p.displayClientMessage(Component.translatable("message.binosvanillaplus.goblin_stole", taken.getHoverName()), true);
        alertPets(p);
        if (ringActive) { removeEffect(MobEffects.INVISIBILITY); ringActive = false; }   // perde a invisibilidade ao furtar
        setPersistenceRequired();
        stealCooldown = STEAL_COOLDOWN;
        runFrom(p, FLEE_AFTER_STEAL);
        giggleIn = 30;
    }

    // os pets do jogador avisam do furto
    private void alertPets(Player owner) {
        for (TamableAnimal pet : level().getEntitiesOfClass(TamableAnimal.class, owner.getBoundingBox().inflate(16),
                a -> a.isTame() && owner.getUUID().equals(a.getOwnerUUID()))) {
            SoundEvent s = pet instanceof Wolf ? SoundEvents.WOLF_GROWL : pet instanceof Cat ? SoundEvents.CAT_HISS
                    : pet instanceof Parrot ? SoundEvents.PARROT_AMBIENT : null;
            if (s != null) pet.playSound(s, 1.2f, 1.0f); else pet.playAmbientSound();
            pet.getLookControl().setLookAt(this);
        }
        for (CompanionAllayEntity allay : level().getEntitiesOfClass(CompanionAllayEntity.class, owner.getBoundingBox().inflate(16),
                a -> owner.getUUID().equals(a.getOwnerId()))) {
            allay.playSound(SoundEvents.ALLAY_HURT, 1.2f, 1.3f);
        }
    }

    // ------------------------------------------------------------------ sons
    @Override @Nullable
    protected SoundEvent getAmbientSound() { return getState() == ST_STALK ? null : ModSounds.CAVE_GOBLIN_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.CAVE_GOBLIN_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.CAVE_GOBLIN_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.7f; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (getState() == ST_STALK) return;   // chega sem fazer barulho
        super.playStepSound(pos, state);
    }

    // ------------------------------------------------------------------ nascer / morrer / salvar
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        hasRing = level.getRandom().nextFloat() < RING_CHANCE;
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (int i = 0; i < sack.size(); i++) {
            if (!sack.get(i).isEmpty()) spawnAtLocation(sack.get(i));
            sack.set(i, ItemStack.EMPTY);
        }
        if (hasRing) spawnAtLocation(new ItemStack(ModItems.INVISIBLE_RING.get()));
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) { return sackEmpty() && !hasRing && super.removeWhenFarAway(distSqr); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag s = new CompoundTag();
        ContainerHelper.saveAllItems(s, sack, registryAccess());
        tag.put("GoblinSack", s);
        tag.putBoolean("GoblinRing", hasRing);
        tag.putInt("GoblinStealCooldown", stealCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ContainerHelper.loadAllItems(tag.getCompound("GoblinSack"), sack, registryAccess());
        hasRing = tag.getBoolean("GoblinRing");
        stealCooldown = tag.getInt("GoblinStealCooldown");
    }

    // ================================================================== OBJETIVOS (IA)

    // Foge de quem o assustou/roubou. Sem saida e com a ameaca colada: se encolhe tremendo.
    static class FleeGoal extends Goal {
        private final CaveGoblinEntity g;
        private int repath;
        FleeGoal(CaveGoblinEntity g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return g.fleeTicks > 0 && g.threat() != null; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; g.setState(ST_FLEE); }
        @Override public void stop() { g.setState(ST_NORMAL); g.getNavigation().stop(); }
        @Override public void tick() {
            Entity t = g.threat();
            if (t == null) return;
            if (--repath <= 0 || g.getNavigation().isDone()) {
                repath = 10;
                Vec3 away = DefaultRandomPos.getPosAway(g, 16, 7, t.position());
                if (away != null) g.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            }
            boolean cornered = g.getNavigation().isDone() && g.distanceToSqr(t) < 9;
            g.setState(cornered ? ST_COWER : ST_FLEE);
        }
    }

    // Espreita: chega por tras sem barulho, congela se o jogador olhar, foge se for pego de perto, furta e corre.
    static class StealGoal extends Goal {
        private final CaveGoblinEntity g;
        @Nullable Player target;
        private int time;
        StealGoal(CaveGoblinEntity g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            if (g.stealCooldown > 0 || g.fleeTicks > 0 || g.sackFull() || g.tickCount % 10 != 0) return false;
            Player best = null; double bd = 16 * 16;
            for (Player p : g.level().getEntitiesOfClass(Player.class, g.getBoundingBox().inflate(16), CaveGoblinEntity::canBeRobbed)) {
                double d = g.distanceToSqr(p);
                if (d < bd && !g.isSeenBy(p)) { bd = d; best = p; }
            }
            target = best;
            return best != null;
        }
        @Override public boolean canContinueToUse() {
            return target != null && canBeRobbed(target) && g.stealCooldown == 0 && g.fleeTicks == 0 && time < 900 && g.distanceToSqr(target) < 20 * 20;
        }
        @Override public void start() { time = 0; g.setState(ST_STALK); }
        @Override public void stop() {
            target = null;
            if (g.getState() == ST_STALK) g.setState(ST_NORMAL);
            g.getNavigation().stop();
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (target == null) return;
            time++;
            g.getLookControl().setLookAt(target, 30.0f, 30.0f);
            double d = g.distanceTo(target);
            if (g.isSeenBy(target)) {
                if (d < 4.5) {   // pego no flagra
                    g.event(EV_STARTLED);
                    g.playSound(ModSounds.CAVE_GOBLIN_STARTLED.get(), 1.0f, 1.0f);
                    g.runFrom(target, 160);
                } else g.getNavigation().stop();   // congela na espreita
                return;
            }
            if (d < 1.6) { g.steal(target); return; }
            if (time % 5 == 0) g.getNavigation().moveTo(target, 0.55);
        }
    }

    // Carne de rato (crua ou cozida) no chao a ate 8 blocos: vai ate ela, pega e come (animacao eat)
    static class EatMeatGoal extends Goal {
        private final CaveGoblinEntity g;
        @Nullable private net.minecraft.world.entity.item.ItemEntity meat;
        private int time;
        EatMeatGoal(CaveGoblinEntity g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private static boolean isMeat(net.minecraft.world.entity.item.ItemEntity i) {
            return i.isAlive() && (i.getItem().is(ModItems.RAT_MEAT.get()) || i.getItem().is(ModItems.COOKED_RAT_MEAT.get()));
        }
        @Override public boolean canUse() {
            if (g.eatTicks > 0 || g.fleeTicks > 0 || g.getState() != ST_NORMAL || g.tickCount % 10 != 0) return false;
            var list = g.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, g.getBoundingBox().inflate(8), EatMeatGoal::isMeat);
            meat = list.isEmpty() ? null : list.get(0);
            return meat != null;
        }
        @Override public boolean canContinueToUse() { return meat != null && isMeat(meat) && g.fleeTicks == 0 && time < 300; }
        @Override public void start() { time = 0; }
        @Override public void stop() { meat = null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (meat == null) return;
            time++;
            g.getLookControl().setLookAt(meat, 30, 30);
            if (time % 5 == 0) g.getNavigation().moveTo(meat, 1.1);
            if (g.distanceToSqr(meat) < 1.5) {
                meat.getItem().shrink(1);
                if (meat.getItem().isEmpty()) meat.discard();
                g.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 0.8f, 1.3f);
                g.eatTicks = 70;
                g.setState(ST_EAT);
                g.getNavigation().stop();
                g.huntCooldown = 2400;
                meat = null;
            }
        }
    }

    // Caca ratos: persegue um rato calmo, mata e come (animacao eat). Depois fica um tempo sem fome.
    static class HuntRatGoal extends Goal {
        private final CaveGoblinEntity g;
        @Nullable private LivingEntity prey;
        private int time;
        HuntRatGoal(CaveGoblinEntity g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            if (g.huntCooldown > 0 || g.fleeTicks > 0 || g.eatTicks > 0 || g.getState() != ST_NORMAL || g.tickCount % 20 != 0) return false;
            prey = g.level().getNearestEntity(LivingEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(10).selector(CaveGoblinEntity::isPrey),
                    g, g.getX(), g.getY(), g.getZ(), g.getBoundingBox().inflate(10));
            return prey != null;
        }
        @Override public boolean canContinueToUse() { return prey != null && prey.isAlive() && isPrey(prey) && g.fleeTicks == 0 && time < 300; }
        @Override public void start() { time = 0; }
        @Override public void stop() {
            g.huntCooldown = prey != null && !prey.isAlive() ? 60 : 400;   // pegou: logo depois vai comer a carne (EatMeatGoal)
            prey = null;
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (prey == null) return;
            time++;
            g.getLookControl().setLookAt(prey, 30, 30);
            if (time % 5 == 0) g.getNavigation().moveTo(prey, 1.25);
            if (g.distanceToSqr(prey) < 1.6 && time % 12 == 0) { g.swing(net.minecraft.world.InteractionHand.MAIN_HAND); g.doHurtTarget(prey); }
        }
    }

    // Na luz forte: cobre os olhos e procura um canto escuro.
    static class SeekDarknessGoal extends Goal {
        private final CaveGoblinEntity g;
        @Nullable private Vec3 dest;
        SeekDarknessGoal(CaveGoblinEntity g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (g.fleeTicks > 0 || g.tickCount % 10 != 0 || g.blockLight() < DARK_LIMIT) return false;
            dest = null; int best = g.blockLight() - 2;
            for (int i = 0; i < 12; i++) {
                Vec3 v = LandRandomPos.getPos(g, 12, 6);
                if (v == null) continue;
                int l = g.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(v));
                if (l < best) { best = l; dest = v; }
            }
            return true;
        }
        @Override public boolean canContinueToUse() { return g.blockLight() >= DARK_LIMIT - 2 && g.fleeTicks == 0; }
        @Override public void start() {
            g.setState(ST_SHIELD);
            if (dest != null) g.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.1);
        }
        @Override public void stop() { if (g.getState() == ST_SHIELD) g.setState(ST_NORMAL); }
        @Override public void tick() { if (g.getNavigation().isDone() && dest != null && g.getRandom().nextInt(40) == 0) start(); }
    }
}
