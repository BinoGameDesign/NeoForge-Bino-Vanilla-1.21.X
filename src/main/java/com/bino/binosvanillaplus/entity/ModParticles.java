package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Partículas novas do mod
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BinosVanillaMod.MOD_ID);

    // Coração rosa (a imagem está em textures/particle/pink_heart.png)
    // false = não é obrigatório mostrar de longe
    public static final Supplier<SimpleParticleType> PINK_HEART =
            PARTICLE_TYPES.register("pink_heart", () -> new SimpleParticleType(false));

    // "Z" que sobe de quem esta dormindo (rato)
    public static final Supplier<SimpleParticleType> SLEEP_Z =
            PARTICLE_TYPES.register("sleep_z", () -> new SimpleParticleType(false));

    // mod Combat: faisca do golpe (voa na direcao do golpe, cai e apaga)
    public static final Supplier<SimpleParticleType> HIT_SPARK =
            PARTICLE_TYPES.register("hit_spark", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
