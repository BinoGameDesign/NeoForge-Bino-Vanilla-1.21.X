package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

// Cuspe do King Rat: bola de queijo podre que cai em arco. Acerto: 6 de dano, veneno (5 s) e nausea (6 s).
// Sem modelo: e' desenhada com particulas (NoopRenderer).
public class KingRatSpitEntity extends ThrowableProjectile {
    private static final DustParticleOptions GOO = new DustParticleOptions(new Vector3f(0.62f, 0.66f, 0.22f), 1.4f);

    public KingRatSpitEntity(EntityType<? extends KingRatSpitEntity> type, Level level) { super(type, level); }

    public KingRatSpitEntity(Level level, LivingEntity owner) {
        super(ModEntities.KING_RAT_SPIT.get(), owner, level);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected double getDefaultGravity() { return 0.035; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++)
                level().addParticle(GOO, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + (random.nextDouble() - 0.5) * 0.3,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
        }
        if (tickCount > 100) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() == getOwner() || hit.getEntity() instanceof RatEntity r && !r.isAlly()) return;
        if (hit.getEntity() instanceof LivingEntity le && le.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), 6f)) {
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), getOwner());
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), getOwner());
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(GOO, getX(), getY(), getZ(), 18, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            playSound(SoundEvents.SLIME_SQUISH, 1f, 0.7f);
            discard();
        }
    }
}
