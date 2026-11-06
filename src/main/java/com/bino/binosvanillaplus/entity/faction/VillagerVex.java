package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import com.bino.binosvanillaplus.faction.FactionMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;

// Espirito Villager: o "vex" que o Mago Villager invoca (cores de allay). Mesmo voo e investida do vex.
public class VillagerVex extends Vex implements FactionMob {
    public VillagerVex(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();   // voo e investida do vex
        targetSelector.removeAllGoals(g -> true);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new CopyOwnerTarget());
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, FactionData::villagerEnemy));
    }

    @Override
    public Faction faction() { return Faction.VILLAGER; }

    // ataca o mesmo alvo do mago que o invocou
    private class CopyOwnerTarget extends TargetGoal {
        private final TargetingConditions cond = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

        CopyOwnerTarget() { super(VillagerVex.this, false); }

        @Override
        public boolean canUse() {
            Mob owner = getOwner();
            return owner != null && owner.getTarget() != null && canAttack(owner.getTarget(), cond);
        }

        @Override
        public void start() {
            Mob owner = getOwner();
            if (owner != null) VillagerVex.this.setTarget(owner.getTarget());
            super.start();
        }
    }
}
