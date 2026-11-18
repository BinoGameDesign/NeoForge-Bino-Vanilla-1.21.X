package com.bino.binosvanillaplus.sound;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Sons do mod (assets/binosvanillaplus/sounds/*.ogg, sintetizados para o mod). Alcance = volume x 16 blocos.
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BinosVanillaMod.MOD_ID);

    public static final Supplier<SoundEvent> WHISTLE = register("whistle");
    public static final Supplier<SoundEvent> SONAR_PING = register("sonar_ping");
    public static final Supplier<SoundEvent> CAMERA_SHUTTER = register("camera_shutter");
    // mesa de lapidacao (cena em primeira pessoa)
    public static final Supplier<SoundEvent> LAPIDARY_SPIN = register("lapidary_spin");
    public static final Supplier<SoundEvent> LAPIDARY_READY = register("lapidary_ready");
    public static final Supplier<SoundEvent> LAPIDARY_DONE = register("lapidary_done");
    public static final Supplier<SoundEvent> LAPIDARY_CONTACT = register("lapidary_contact");

    // Cave Goblin (por enquanto apontam para sons do jogo com outro tom, em sounds.json; trocar pelos gravados depois)
    public static final Supplier<SoundEvent> CAVE_GOBLIN_AMBIENT = register("cave_goblin.ambient");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_HURT = register("cave_goblin.hurt");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_DEATH = register("cave_goblin.death");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_STEAL = register("cave_goblin.steal");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_GIGGLE = register("cave_goblin.giggle");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_STARTLED = register("cave_goblin.startled");
    public static final Supplier<SoundEvent> CAVE_GOBLIN_RING = register("cave_goblin.ring");

    // Rato: sons do "Squeaky Rat" de qubodup (CC0) com variacoes de tom/tempo; eat ainda e provisorio
    public static final Supplier<SoundEvent> RAT_AMBIENT = register("rat.ambient");
    public static final Supplier<SoundEvent> RAT_HURT = register("rat.hurt");
    public static final Supplier<SoundEvent> RAT_DEATH = register("rat.death");
    public static final Supplier<SoundEvent> RAT_SQUEAK = register("rat.squeak");
    public static final Supplier<SoundEvent> RAT_EAT = register("rat.eat");
    public static final Supplier<SoundEvent> RAT_ATTACK = register("rat.attack");
    public static final Supplier<SoundEvent> RAT_TRAP_SNAP = register("rat_trap.snap");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name)));
    }

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
