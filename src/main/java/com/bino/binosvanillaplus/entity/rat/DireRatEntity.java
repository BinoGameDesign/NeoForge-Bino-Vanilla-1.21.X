package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.block.custom.CheeseBlock;
import com.bino.binosvanillaplus.leveling.RegionDifficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

// Rato Atroz (Dire Rat): rato do tamanho de um porco, sempre agressivo. Usa o modelo e as animacoes do rato.
//  - Nasce nos mesmos lugares que os ratos (cavernas escuras e estruturas), dividindo o limite de monstros,
//    so em regioes de nivel 15 ou mais.
//  - Atraido so pelo queijo grande (bloco) no chao; queijo na mao e ratoeira nao atraem.
//  - Nao morre na ratoeira nem cabe na gaiola. Foge de gatos e jaguatiricas.
//  - Quando agra um jogador, os ratos por perto atacam o mesmo jogador.
public class DireRatEntity extends RatEntity implements Enemy {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final double MIN_LEVEL = 15;   // nivel da regiao para nascer
    public static final int RALLY_RANGE = 16;    // ratos a ate tantos blocos vem junto

    public DireRatEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.SCALE, 2.4);   // rato x2,4 = mais ou menos um porco
    }

    @Override protected boolean hasVariants() { return false; }
    @Override public boolean isAngry() { return true; }
    @Override public boolean shouldDespawnInPeaceful() { return true; }
    @Override public float getVoicePitch() { return 0.55f + random.nextFloat() * 0.1f; }   // guincho grave

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeGoal(this));
        goalSelector.addGoal(2, new EatCakeGoal(this, st -> st.getBlock() instanceof CheeseBlock));   // so o queijo grande
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // so gatos e jaguatiricas assustam
    @Override
    boolean scares(LivingEntity e) { return e instanceof Cat || e instanceof Ocelot; }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target instanceof Player p && !level().isClientSide) callRats(p);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 40 == 0 && getTarget() instanceof Player p && p.isAlive()) callRats(p);   // quem chegar depois vem junto
    }

    private void callRats(Player p) {
        if (p.isCreative() || p.isSpectator()) return;
        for (RatEntity r : level().getEntitiesOfClass(RatEntity.class, new AABB(blockPosition()).inflate(RALLY_RANGE),
                r -> !(r instanceof DireRatEntity) && r.isAlive())) r.rally(p);
    }

    // ------------------------------------------------------------------ onde nasce
    public static boolean checkSpawn(EntityType<DireRatEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        if (MobSpawnType.isSpawner(reason) || reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.EVENT) return true;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (RegionDifficulty.levelAt(level.getLevel(), pos) < MIN_LEVEL) return false;
        if (!Monster.isDarkEnoughToSpawn(level, pos, random)) return false;
        // caverna (sem ceu, abaixo de Y 40) ou dentro de estrutura dos ratos
        return (pos.getY() < RatSpawning.CAVE_MAX_Y && !level.canSeeSky(pos)) || RatSpawning.inRatStructure(level.getLevel(), pos);
    }
}
