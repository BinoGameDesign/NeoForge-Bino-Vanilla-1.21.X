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

// Rato Gigante (Dire Rat): rato do tamanho de um porco, sempre agressivo. Modelo proprio (dire_rat.bbmodel) com os ossos
// do rato; tem a versao das profundezas (abaixo de Y -32, como o rato). Ataques: mordida com bote (salta no alvo) e,
// de vez em quando, chicotada de cauda de medio alcance.
//  - Nasce nos mesmos lugares que os ratos (cavernas escuras e estruturas), dividindo o limite de monstros,
//    so em regioes de nivel 15 ou mais.
//  - Atraido so pelo queijo grande (bloco) no chao; queijo na mao e ratoeira nao atraem.
//  - Nao morre na ratoeira nem cabe na gaiola. Foge de gatos e jaguatiricas.
//  - Quando agra um jogador, os ratos por perto atacam o mesmo jogador.
public class DireRatEntity extends RatEntity implements Enemy {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final double MIN_LEVEL = 15;   // nivel da regiao para nascer
    public static final int RALLY_RANGE = 16;    // ratos a ate tantos blocos vem junto
    public static final float WHIP_DAMAGE = 6.0f;
    public static final double WHIP_MIN = 2.2, WHIP_MAX = 4.6;   // alcance da chicotada (blocos)
    public static final int WHIP_COOLDOWN_MIN = 20 * 7, WHIP_COOLDOWN_MAX = 20 * 15;
    public static final int BITE_INTERVAL = 14;                  // ticks entre mordidas (zumbi: 20)

    private int whipCooldown = WHIP_COOLDOWN_MIN;

    public DireRatEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 2.4);   // rato x2,4 = mais ou menos um porco
    }

    @Override protected boolean canSwarm() { return false; }

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return isDeep() ? net.minecraft.network.chat.Component.translatable("entity.binosvanillaplus.deep_dire_rat") : getType().getDescription();
    }

    // duracao das animacoes proprias (tem que bater com o dire_rat.bbmodel / DireRatAnimations)
    @Override
    protected int oneShotLength(String a) {
        return switch (a) {
            case "attack" -> 11; case "tail_whip" -> 20;
            default -> super.oneShotLength(a);
        };
    }
    @Override public boolean isAngry() { return true; }
    @Override public boolean shouldDespawnInPeaceful() { return true; }
    @Override public float getVoicePitch() { return 0.55f + random.nextFloat() * 0.1f; }   // guincho grave

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeGoal(this));
        goalSelector.addGoal(2, new EatCakeGoal(this, st -> st.getBlock() instanceof CheeseBlock));   // so o queijo grande
        goalSelector.addGoal(2, new TailWhipGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, 0.38f));   // bote
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.45, true) {
            @Override protected int getAttackInterval() { return adjustedTickDelay(BITE_INTERVAL); }
        });
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

    // ------------------------------------------------------------------ chicotada de cauda
    // de vez em quando, com o alvo a media distancia: para, gira o corpo e a cauda varre a frente (~0,5 s depois)
    static class TailWhipGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final DireRatEntity r;
        private int t;
        TailWhipGoal(DireRatEntity r) { this.r = r; setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (r.whipCooldown > 0) { r.whipCooldown--; return false; }
            LivingEntity tg = r.getTarget();
            if (tg == null || !tg.isAlive() || !r.onGround() || r.getState() != ST_NORMAL) return false;
            double d = r.distanceTo(tg);
            return d >= WHIP_MIN && d <= WHIP_MAX && r.hasLineOfSight(tg) && r.getRandom().nextInt(3) == 0;
        }
        @Override public boolean canContinueToUse() { return t < 20 && r.isAlive(); }
        @Override public void start() {
            t = 0;
            r.getNavigation().stop();
            r.event(EV_TAIL_WHIP);
            r.playSound(com.bino.binosvanillaplus.sound.ModSounds.RAT_SQUEAK.get(), 1.0f, 0.5f);
        }
        @Override public void tick() {
            t++;
            r.getNavigation().stop();
            LivingEntity tg = r.getTarget();
            if (tg != null && t < 8) r.getLookControl().setLookAt(tg, 30, 30);
            if (t == 10) {   // a cauda passa pela frente: pega quem estiver no alcance
                r.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
                net.minecraft.world.phys.Vec3 c = r.position();
                for (LivingEntity e : r.level().getEntitiesOfClass(LivingEntity.class, r.getBoundingBox().inflate(WHIP_MAX),
                        e -> e != r && e.isAlive() && !(e instanceof RatEntity) && e.distanceTo(r) <= WHIP_MAX)) {
                    if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                    if (e != tg && !(e instanceof Player)) continue;   // so o alvo e jogadores no caminho
                    if (e.hurt(r.damageSources().mobAttack(r), WHIP_DAMAGE)) {
                        net.minecraft.world.phys.Vec3 away = e.position().subtract(c).normalize();
                        e.knockback(1.1, -away.x, -away.z);
                    }
                }
            }
        }
        @Override public void stop() {
            r.whipCooldown = WHIP_COOLDOWN_MIN + r.getRandom().nextInt(WHIP_COOLDOWN_MAX - WHIP_COOLDOWN_MIN);
        }
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
