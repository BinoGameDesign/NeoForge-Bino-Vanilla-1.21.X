package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BinosVanillaMod.MOD_ID);

    public static final Supplier<EntityType<CyalumeStickEntity>> CYALUME_STICK =
            ENTITY_TYPES.register("cyalume_stick",
                    () -> EntityType.Builder.<CyalumeStickEntity>of(CyalumeStickEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)        // tamanho da hitbox
                            .clientTrackingRange(4)     // a que distância os outros jogadores a veem
                            .updateInterval(10)         // de quantos em quantos ticks sincroniza a posição
                            .build("cyalume_stick"));

    // noSave() = o jogo nunca grava esta entidade no mundo
    public static final Supplier<EntityType<CompanionAllayEntity>> COMPANION_ALLAY =
            ENTITY_TYPES.register("companion_allay",
                    () -> EntityType.Builder.<CompanionAllayEntity>of(CompanionAllayEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.6f)       // mesmo tamanho do Allay (o atributo SCALE diminui 30%)
                            .eyeHeight(0.36f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .build("companion_allay"));

    // Allay fantasma que fica parado em cima do tumulo (nao e salvo: o tumulo recria quando o chunk carrega)
    public static final Supplier<EntityType<GraveAllayEntity>> GRAVE_ALLAY =
            ENTITY_TYPES.register("grave_allay",
                    () -> EntityType.Builder.<GraveAllayEntity>of(GraveAllayEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.6f)
                            .eyeHeight(0.36f)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .fireImmune()
                            .noSave()
                            .build("grave_allay"));

    // Faccoes: golem de ferro enegrecido (lado pillager) e vingador villager (lado villager)
    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.PillagerGolem>> PILLAGER_GOLEM =
            ENTITY_TYPES.register("pillager_golem",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.PillagerGolem>of(com.bino.binosvanillaplus.entity.faction.PillagerGolem::new, MobCategory.MISC)
                            .sized(1.4f, 2.7f)
                            .clientTrackingRange(10)
                            .build("pillager_golem"));

    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.VillagerGuard>> VILLAGER_GUARD =
            ENTITY_TYPES.register("villager_guard",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.VillagerGuard>of(com.bino.binosvanillaplus.entity.faction.VillagerGuard::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .passengerAttachments(2.0f)
                            .ridingOffset(-0.6f)
                            .clientTrackingRange(8)
                            .build("villager_guard"));

    // Faccoes, etapa 2 (mesmos tamanhos dos mobs do jogo em que se baseiam)
    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.VillagerMage>> VILLAGER_MAGE =
            ENTITY_TYPES.register("villager_mage",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.VillagerMage>of(com.bino.binosvanillaplus.entity.faction.VillagerMage::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f).passengerAttachments(2.0f).ridingOffset(-0.6f).clientTrackingRange(8)
                            .build("villager_mage"));

    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.VillagerVex>> VILLAGER_VEX =
            ENTITY_TYPES.register("villager_vex",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.VillagerVex>of(com.bino.binosvanillaplus.entity.faction.VillagerVex::new, MobCategory.MONSTER)
                            .fireImmune().sized(0.4f, 0.8f).eyeHeight(0.51875f).passengerAttachments(0.7375f).ridingOffset(0.04f).clientTrackingRange(8)
                            .build("villager_vex"));

    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.VillagerWitch>> VILLAGER_WITCH =
            ENTITY_TYPES.register("villager_witch",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.VillagerWitch>of(com.bino.binosvanillaplus.entity.faction.VillagerWitch::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f).eyeHeight(1.62f).passengerAttachments(2.2625f).clientTrackingRange(8)
                            .build("villager_witch"));

    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.VillagerRavager>> VILLAGER_RAVAGER =
            ENTITY_TYPES.register("villager_ravager",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.VillagerRavager>of(com.bino.binosvanillaplus.entity.faction.VillagerRavager::new, MobCategory.MONSTER)
                            .sized(1.95f, 2.2f).passengerAttachments(new net.minecraft.world.phys.Vec3(0.0, 2.2625, -0.0625)).clientTrackingRange(10)
                            .build("villager_ravager"));

    public static final Supplier<EntityType<com.bino.binosvanillaplus.entity.faction.PillagerSmuggler>> PILLAGER_SMUGGLER =
            ENTITY_TYPES.register("pillager_smuggler",
                    () -> EntityType.Builder.<com.bino.binosvanillaplus.entity.faction.PillagerSmuggler>of(com.bino.binosvanillaplus.entity.faction.PillagerSmuggler::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10)
                            .build("pillager_smuggler"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}