package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import com.bino.binosvanillaplus.faction.FactionMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Mago Villager: o evocador do lado dos villagers. Mesmos feiticos (presas e invocacao),
// mas os espiritos que ele chama sao Espiritos Villager (ver FactionAI) e ele luta contra o lado pillager.
public class VillagerMage extends Evoker implements FactionMob {
    public VillagerMage(EntityType<? extends Evoker> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();   // mantem os feiticos do evocador
        goalSelector.removeAllGoals(g -> g instanceof AvoidEntityGoal<?>);
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, t -> t instanceof Player p && FactionData.get(p) == Faction.PILLAGER,
                8f, 0.6, 1.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
        targetSelector.removeAllGoals(g -> true);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, FactionData::villagerEnemy));
    }

    @Override
    public Faction faction() { return Faction.VILLAGER; }

    // aliado pela faccao (e dos proprios espiritos), nao pela "amizade" dos illagers
    @Override
    public boolean isAlliedTo(Entity e) {
        if (e == this) return true;
        if (e instanceof Vex v && v.getOwner() == this) return true;
        return FactionData.allied(this, e) || isAlliedTo(e.getTeam());
    }

    @Override
    public boolean canBeLeader() { return false; }

    @Override
    public boolean canJoinRaid() { return false; }

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
