package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Ocelot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Ratos e os mobs do jogo: gatos e jaguatiricas cacam ratos; golems de ferro morrem de medo deles.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class RatWorldEvents {
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (ent instanceof Cat c) c.goalSelector.addGoal(2, new HuntRatGoal(c));
        else if (ent instanceof Ocelot o) o.goalSelector.addGoal(2, new HuntRatGoal(o));
        else if (ent instanceof IronGolem g) g.goalSelector.addGoal(0, new AvoidEntityGoal<>(g, RatEntity.class, 8.0f, 1.0, 1.3,
                r -> !(r instanceof com.bino.binosvanillaplus.entity.rat.DireRatEntity)));   // do Rato Atroz o golem nao foge: briga
    }

    // persegue o rato mais perto (ate 12 blocos), corre atras e da o bote. Gato sentado/domado sentado nao caca.
    static class HuntRatGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final net.minecraft.world.entity.PathfinderMob cat;
        @javax.annotation.Nullable private RatEntity prey;
        private int time, cooldown;
        HuntRatGoal(net.minecraft.world.entity.PathfinderMob cat) { this.cat = cat; setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean busy() { return cat instanceof net.minecraft.world.entity.TamableAnimal t && t.isOrderedToSit(); }
        @Override public boolean canUse() {
            if (cooldown > 0) { cooldown--; return false; }
            if (busy() || cat.getRandom().nextInt(10) != 0) return false;
            prey = cat.level().getNearestEntity(RatEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(12)
                            .selector(r -> !(r instanceof com.bino.binosvanillaplus.entity.rat.DireRatEntity)),   // o Rato Atroz e' grande demais
                    cat, cat.getX(), cat.getY(), cat.getZ(), cat.getBoundingBox().inflate(12));
            return prey != null;
        }
        @Override public boolean canContinueToUse() { return prey != null && prey.isAlive() && !busy() && time < 400 && cat.distanceToSqr(prey) < 20 * 20; }
        @Override public void start() { time = 0; }
        @Override public void stop() { prey = null; cooldown = 100; cat.getNavigation().stop(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (prey == null) return;
            time++;
            cat.getLookControl().setLookAt(prey, 30, 30);
            if (time % 5 == 0) cat.getNavigation().moveTo(prey, 1.4);
            double d = cat.distanceToSqr(prey);
            if (d < 2.5 && d > 0.8 && cat.onGround() && time % 20 == 0) {   // bote
                net.minecraft.world.phys.Vec3 v = prey.position().subtract(cat.position()).normalize().scale(0.45);
                cat.setDeltaMovement(v.x, 0.35, v.z);
            }
            if (d < 1.3 && time % 10 == 0) { cat.swing(net.minecraft.world.InteractionHand.MAIN_HAND); cat.doHurtTarget(prey); }
        }
    }
}
