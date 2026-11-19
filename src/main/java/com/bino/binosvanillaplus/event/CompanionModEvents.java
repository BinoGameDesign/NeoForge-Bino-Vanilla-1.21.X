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
        // Rato: cavernas (abaixo de Y 0), sem ceu, sem luz de bloco
        event.register(ModEntities.RAT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random)
                        && (net.minecraft.world.entity.MobSpawnType.isSpawner(reason) || reason == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG
                            || reason == net.minecraft.world.entity.MobSpawnType.EVENT
                            || (pos.getY() < 40 && !level.canSeeSky(pos) && level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) == 0)),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Rato Gigante: cavernas escuras ou estruturas dos ratos, so em regiao de nivel 15+ (ver DireRatEntity.checkSpawn)
        event.register(ModEntities.DIRE_RAT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.bino.binosvanillaplus.entity.rat.DireRatEntity::checkSpawn,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Cave Goblin: so abaixo de Y 0, sem ceu, sem luz de bloco, chao firme (ovos e spawners ignoram a regra de luz/altura)
        event.register(ModEntities.CAVE_GOBLIN.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random)
                        && (net.minecraft.world.entity.MobSpawnType.isSpawner(reason) || reason == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG
                            || (pos.getY() < 40 && !level.canSeeSky(pos) && level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) == 0)),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VILLAGER_GUARD.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // a Bruxa Villager nasce na Cabana da Bruxa Villager (igual a bruxa na cabana do pantano)
        event.register(ModEntities.VILLAGER_WITCH.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CAVE_GOBLIN.get(), com.bino.binosvanillaplus.entity.goblin.CaveGoblinEntity.createAttributes().build());
        event.put(ModEntities.RAT.get(), com.bino.binosvanillaplus.entity.rat.RatEntity.createAttributes().build());
        event.put(ModEntities.DIRE_RAT.get(), com.bino.binosvanillaplus.entity.rat.DireRatEntity.createAttributes().build());
        event.put(ModEntities.MIMIC.get(), com.bino.binosvanillaplus.mimic.MimicEntity.createAttributes(false).build());
        event.put(ModEntities.MIMIC_DOUBLE.get(), com.bino.binosvanillaplus.mimic.MimicEntity.createAttributes(true).build());
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