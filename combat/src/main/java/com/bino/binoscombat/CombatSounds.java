package com.bino.binoscombat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Sons do Combat (assets/binoscombat/sounds.json e sounds/combat/)
public final class CombatSounds {
    private CombatSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BinosCombat.MOD_ID);

    public static final Supplier<SoundEvent> SWORD_SWING = register("combat.sword_swing");
    public static final Supplier<SoundEvent> SWORD_HIT = register("combat.sword_hit");
    public static final Supplier<SoundEvent> SWORD_HIT_HARD = register("combat.sword_hit_hard");
    public static final Supplier<SoundEvent> AXE_SWING = register("combat.axe_swing");
    public static final Supplier<SoundEvent> AXE_HIT = register("combat.axe_hit");
    public static final Supplier<SoundEvent> AXE_HIT_HARD = register("combat.axe_hit_hard");
    public static final Supplier<SoundEvent> HIT_WATER = register("combat.hit_water");
    public static final Supplier<SoundEvent> PARRY = register("combat.parry");
    public static final Supplier<SoundEvent> PARRY_PERFECT = register("combat.parry_perfect");
    public static final Supplier<SoundEvent> ISSEN_WARP = register("combat.issen_warp");   // teleporte do Issen em cadeia (tools/sounds/gen_issen_warp.py)

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BinosCombat.MOD_ID, name)));
    }

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
