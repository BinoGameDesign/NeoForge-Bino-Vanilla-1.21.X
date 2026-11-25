package com.bino.binoscombat;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Particulas do Combat (texturas em assets/binoscombat/textures/particle; desenho em client/combat)
public final class CombatParticles {
    private CombatParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, BinosCombat.MOD_ID);

    // faisca do golpe (voa na direcao do golpe, cai e apaga)
    public static final Supplier<SimpleParticleType> HIT_SPARK =
            PARTICLE_TYPES.register("hit_spark", () -> new SimpleParticleType(true));

    // visual estilo Zelda
    public static final Supplier<SimpleParticleType> PARRY_SPARK =      // faisca dourada do parry
            PARTICLE_TYPES.register("parry_spark", () -> new SimpleParticleType(true));
    public static final Supplier<SimpleParticleType> IMPACT_STAR =      // estrela de 4 pontas no impacto (vx = tamanho)
            PARTICLE_TYPES.register("impact_star", () -> new SimpleParticleType(true));
    public static final Supplier<SimpleParticleType> IMPACT_RING =      // anel que abre (vx = tamanho)
            PARTICLE_TYPES.register("impact_ring", () -> new SimpleParticleType(true));
    public static final Supplier<SimpleParticleType> DEATH_PUFF =       // fumaca roxa do mob morrendo
            PARTICLE_TYPES.register("death_puff", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) { PARTICLE_TYPES.register(bus); }
}
