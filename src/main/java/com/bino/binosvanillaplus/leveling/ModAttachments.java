package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

// Dados extras guardados nas entidades: o nivel do mob (0 = ainda sem nivel)
public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BinosVanillaMod.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> MOB_LEVEL = ATTACHMENTS.register("mob_level",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    // slots extras do jogador (brincos, colar, anel, longo alcance, costas)
    public static final Supplier<AttachmentType<com.bino.binosvanillaplus.inventory.ExtraSlots>> EXTRA_SLOTS = ATTACHMENTS.register("extra_slots",
            () -> AttachmentType.serializable(com.bino.binosvanillaplus.inventory.ExtraSlots::new).copyOnDeath().build());

    // faccao do jogador (0 = ainda nao escolheu; ver faction/Faction)
    public static final Supplier<AttachmentType<Integer>> FACTION = ATTACHMENTS.register("faction",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    // villager "Pillager civil" (mesmo villager do jogo, mas do lado dos pillagers)
    public static final Supplier<AttachmentType<Boolean>> PILLAGER_CIVILIAN = ATTACHMENTS.register("pillager_civilian",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    // villager ja conferido (nasceu ou nao numa Vila Pillager)
    public static final Supplier<AttachmentType<Boolean>> FACTION_CHECKED = ATTACHMENTS.register("faction_checked",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }
}
