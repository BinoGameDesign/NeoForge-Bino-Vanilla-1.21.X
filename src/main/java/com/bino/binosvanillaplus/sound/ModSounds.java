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

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name)));
    }

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
