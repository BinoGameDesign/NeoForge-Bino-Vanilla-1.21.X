package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

// Vingador Villager: o "vindicator" do lado dos villagers (guarda com machado).
// Ataca illagers, ravagers, bruxas, vex, golens pillager e jogadores Pillager; nunca entra em raid dos pillagers.
public class VillagerGuard extends Vindicator {
    public VillagerGuard(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                t -> t instanceof Player p && FactionData.get(p) == Faction.PILLAGER));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Raider.class, 10, true, false, t -> !(t instanceof VillagerGuard)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Vex.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PillagerGolem.class, true));
    }

    @Override
    public boolean canJoinRaid() { return false; }

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
