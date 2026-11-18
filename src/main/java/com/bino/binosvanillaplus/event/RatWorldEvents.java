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
        if (ent instanceof Cat c) c.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(c, RatEntity.class, false));
        else if (ent instanceof Ocelot o) o.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(o, RatEntity.class, false));
        else if (ent instanceof IronGolem g) g.goalSelector.addGoal(0, new AvoidEntityGoal<>(g, RatEntity.class, 8.0f, 1.0, 1.3));
    }
}
