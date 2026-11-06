package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import com.bino.binosvanillaplus.faction.FactionMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

// Ravager Villager: o mesmo ravager (so as cores mudam), lutando pela vila.
public class VillagerRavager extends Ravager implements FactionMob {
    public VillagerRavager(EntityType<? extends Ravager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();   // investida e rugido do ravager
        targetSelector.removeAllGoals(g -> true);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, FactionData::villagerEnemy));
    }

    @Override
    public Faction faction() { return Faction.VILLAGER; }

    @Override
    public boolean canJoinRaid() { return false; }

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
