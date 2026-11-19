package com.bino.binosvanillaplus.mimic;

import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// O mimico depois de revelado: o bau que pula atras do jogador mordendo. Guarda os itens que estavam no bau (solta ao
// morrer). Comeca abrindo a boca (revelar) e da a MORDIDA forte em quem tentou pegar o item; depois persegue aos pulos.
// Sem alvo por um tempo e no chao: vira bau de novo (mimico simples), com os itens, esperando a proxima vitima.
public class MimicEntity extends Monster {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final float REVEAL_BITE = 16f;   // a mordida da revelacao (MUITO dano; armadura reduz)
    public static final int REVEAL_TICKS = 18;     // abre a boca e espera ate morder
    public static final double REACH = 1.9;       // alcance da mordida normal
    public static final int BITE_COOLDOWN = 24;
    public static final int REST_TICKS = 20 * 30;  // sem alvo por 30 s: volta a ser bau

    public static final byte EV_REVEAL = 60, EV_BITE = 61, EV_HOP = 62;

    private static final EntityDataAccessor<Boolean> TRAPPED = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState reveal = new AnimationState(), bite = new AnimationState(), hop = new AnimationState();
    private final List<ItemStack> loot = new ArrayList<>();
    private int revealT = -1, biteIn = -1, hopCd, biteCd, idle;
    @Nullable private Player firstVictim;

    public MimicEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes(boolean dbl) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, dbl ? 70.0 : 40.0)
                .add(Attributes.ATTACK_DAMAGE, dbl ? 12.0 : 8.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public boolean isDouble() { return getType() == ModEntities.MIMIC_DOUBLE.get(); }
    public boolean isTrapped() { return entityData.get(TRAPPED); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TRAPPED, false);
    }

    public void setup(List<ItemStack> items, boolean trapped, Player victim) {
        loot.clear(); loot.addAll(items);
        entityData.set(TRAPPED, trapped);
        firstVictim = victim;
        setTarget(victim);
        revealT = 0;
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    void event(byte id) { level().broadcastEntityEvent(this, id); }

    // ------------------------------------------------------------------ cerebro (no servidor)
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity t = getTarget();
        if (t != null && t.isAlive()) face(t);

        if (revealT >= 0) {   // revelacao: abre a boca, morde quem tentou pegar o item
            if (revealT == 0) {   // o susto: rugido + grunhido grave
                event(EV_REVEAL);
                playSound(SoundEvents.CHEST_OPEN, 1.0f, 0.5f);
                playSound(SoundEvents.RAVAGER_ROAR, 1.6f, 1.2f);
                playSound(SoundEvents.HOGLIN_ANGRY, 1.2f, 0.5f);
            }
            if (revealT == REVEAL_TICKS - 4) { event(EV_BITE); }
            if (revealT == REVEAL_TICKS) {
                playSound(SoundEvents.CHEST_CLOSE, 1.4f, 0.6f);
                playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
                playSound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.75f);
                if (firstVictim != null && firstVictim.isAlive() && distanceTo(firstVictim) < 3.5
                        && firstVictim.hurt(damageSources().mobAttack(this), REVEAL_BITE)) {
                    Vec3 away = firstVictim.position().subtract(position()).normalize();
                    firstVictim.knockback(0.8, -away.x, -away.z);
                }
                revealT = -1; hopCd = 10;
                return;
            }
            revealT++;
            return;
        }

        if (biteIn > 0 && --biteIn == 0 && t != null && distanceTo(t) <= REACH + 0.6) doHurtTarget(t);
        if (biteCd > 0) biteCd--;
        if (hopCd > 0) hopCd--;
        if (t == null || !t.isAlive()) {
            if (onGround() && ++idle > REST_TICKS && !isDouble()) becomeChest();
            return;
        }
        idle = 0;
        double d = distanceTo(t);
        if (d <= REACH && biteCd == 0) {
            event(EV_BITE); biteIn = 4; biteCd = BITE_COOLDOWN;
            playSound(SoundEvents.CHEST_CLOSE, 1.0f, 0.8f);
            playSound(SoundEvents.HOGLIN_ATTACK, 0.9f, 0.6f + random.nextFloat() * 0.15f);
        } else if (onGround() && hopCd == 0 && d > REACH * 0.8) {   // pulo na direcao do alvo
            Vec3 dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
            double sp = isDouble() ? 0.36 : 0.42;
            setDeltaMovement(dir.x * sp, 0.46, dir.z * sp);
            hasImpulse = true;
            event(EV_HOP);
            playSound(SoundEvents.CHEST_OPEN, 0.6f, 0.7f + random.nextFloat() * 0.2f);
            if (random.nextFloat() < 0.35f) playSound(SoundEvents.HOGLIN_AMBIENT, 0.8f, 0.55f + random.nextFloat() * 0.15f);
            hopCd = 14 + random.nextInt(8);
        }
    }

    private void face(LivingEntity t) {
        float yaw = (float) (Mth.atan2(t.getZ() - getZ(), t.getX() - getX()) * (180f / Math.PI)) - 90f;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 20f));
        yBodyRot = getYRot(); yHeadRot = getYRot();
    }

    // volta a ser bau (com os itens) e espera a proxima vitima
    private void becomeChest() {
        BlockPos p = blockPosition();
        if (!level().getBlockState(p).canBeReplaced()) return;
        Direction f = Direction.fromYRot(getYRot());
        level().setBlock(p, (isTrapped() ? Blocks.TRAPPED_CHEST : Blocks.CHEST).defaultBlockState().setValue(ChestBlock.FACING, f), 3);
        if (level().getBlockEntity(p) instanceof ChestBlockEntity be) {
            for (int i = 0; i < loot.size() && i < be.getContainerSize(); i++) be.setItem(i, loot.get(i));
            be.setData(ModAttachments.MIMIC, true);
            be.setChanged();
            loot.clear();
        }
        discard();
    }

    // ------------------------------------------------------------------ cliente
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_REVEAL -> reveal.start(tickCount);
            case EV_BITE -> bite.start(tickCount);
            case EV_HOP -> hop.start(tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ resto
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    // depois de revelado: grunhidos graves de monstro
    @Override @Nullable protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override public float getVoicePitch() { return 0.6f + random.nextFloat() * 0.15f; }
    @Override public int getAmbientSoundInterval() { return 100; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR; }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack s : loot) spawnAtLocation(s);
        loot.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MimicTrapped", isTrapped());
        ListTag list = new ListTag();
        for (ItemStack s : loot) if (!s.isEmpty()) list.add(s.save(registryAccess()));
        tag.put("MimicLoot", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TRAPPED, tag.getBoolean("MimicTrapped"));
        loot.clear();
        for (Tag t : tag.getList("MimicLoot", Tag.TAG_COMPOUND)) ItemStack.parse(registryAccess(), t).ifPresent(loot::add);
    }
}
