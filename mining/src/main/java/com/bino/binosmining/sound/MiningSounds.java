package com.bino.binosmining.sound;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MiningSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BinosMining.MOD_ID);

    // mesa de lapidacao (cena em primeira pessoa)
    public static final Supplier<SoundEvent> LAPIDARY_SPIN = register("lapidary_spin");
    public static final Supplier<SoundEvent> LAPIDARY_READY = register("lapidary_ready");
    public static final Supplier<SoundEvent> LAPIDARY_DONE = register("lapidary_done");
    public static final Supplier<SoundEvent> LAPIDARY_CONTACT = register("lapidary_contact");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, name)));
    }

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
