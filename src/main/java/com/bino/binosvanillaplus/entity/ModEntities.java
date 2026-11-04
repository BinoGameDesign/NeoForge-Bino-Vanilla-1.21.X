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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}