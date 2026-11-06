package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import com.bino.binosvanillaplus.faction.FactionMob;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Bruxa Villager: joga pocoes nos inimigos da vila e cura os aliados feridos (villagers, guardas, jogadores Villager).
public class VillagerWitch extends Witch implements FactionMob {
    public VillagerWitch(EntityType<? extends Witch> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();   // ataque a distancia e beber pocoes
        goalSelector.addGoal(1, new HealAllies());
        targetSelector.removeAllGoals(g -> true);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, FactionData::villagerEnemy));
    }

    @Override
    public Faction faction() { return Faction.VILLAGER; }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (isDrinkingPotion()) return;
        Vec3 mv = target.getDeltaMovement();
        double dx = target.getX() + mv.x - getX();
        double dy = target.getEyeY() - 1.1 - getY();
        double dz = target.getZ() + mv.z - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        Holder<Potion> potion;
        if (FactionData.allied(this, target)) potion = target.getHealth() <= 6f ? Potions.HEALING : Potions.REGENERATION;
        else if (dist >= 8 && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) potion = Potions.SLOWNESS;
        else if (target.getHealth() >= 8f && !target.hasEffect(MobEffects.POISON)) potion = Potions.POISON;
        else if (dist <= 3 && !target.hasEffect(MobEffects.WEAKNESS) && random.nextFloat() < 0.25f) potion = Potions.WEAKNESS;
        else potion = Potions.HARMING;
        ThrownPotion tp = new ThrownPotion(level(), this);
        tp.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, potion));
        tp.setXRot(tp.getXRot() - 20f);
        tp.shoot(dx, dy + dist * 0.2, dz, 0.75f, 8f);
        if (!isSilent()) level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, getSoundSource(), 1f, 0.8f + random.nextFloat() * 0.4f);
        level().addFreshEntity(tp);
    }

    @Override
    public boolean canJoinRaid() { return false; }

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    // de vez em quando joga cura num aliado ferido por perto
    private class HealAllies extends Goal {
        private int cooldown;
        private LivingEntity ally;

        HealAllies() { setFlags(EnumSet.of(Goal.Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || isDrinkingPotion()) return false;
            cooldown = 50;
            List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10),
                    t -> t != VillagerWitch.this && t.isAlive() && FactionData.side(t) == Faction.VILLAGER
                            && t.getHealth() < t.getMaxHealth() * 0.6f && hasLineOfSight(t));
            ally = list.isEmpty() ? null : list.get(0);
            return ally != null;
        }

        @Override
        public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            getLookControl().setLookAt(ally);
            performRangedAttack(ally, 1f);
        }
    }
}
