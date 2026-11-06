package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class CompanionModEvents {

    // o guarda villager nasce no Posto Villager (como os pillagers no posto deles), em qualquer luz
    @SubscribeEvent
    public static void spawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.VILLAGER_GUARD.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPANION_ALLAY.get(),
                Allay.createAttributes()                 // parte dos atributos do Allay normal
                        .add(Attributes.FLYING_SPEED, 0.15)   // 0,1 do Allay normal x 1,5 = 50% mais rápido
                        .add(Attributes.SCALE, 0.7)           // 30% menor
                        .build());
        event.put(ModEntities.PILLAGER_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes().build());
        event.put(ModEntities.VILLAGER_MAGE.get(), net.minecraft.world.entity.monster.Evoker.createAttributes().build());
        event.put(ModEntities.VILLAGER_VEX.get(), net.minecraft.world.entity.monster.Vex.createAttributes().build());
        event.put(ModEntities.VILLAGER_WITCH.get(), net.minecraft.world.entity.monster.Witch.createAttributes().build());
        event.put(ModEntities.VILLAGER_RAVAGER.get(), net.minecraft.world.entity.monster.Ravager.createAttributes().build());
        event.put(ModEntities.PILLAGER_SMUGGLER.get(), net.minecraft.world.entity.Mob.createMobAttributes().build());
        event.put(ModEntities.VILLAGER_GUARD.get(), net.minecraft.world.entity.monster.Vindicator.createAttributes().build());
        event.put(ModEntities.GRAVE_ALLAY.get(),
                Allay.createAttributes().add(Attributes.SCALE, 0.7).build());
    }
}