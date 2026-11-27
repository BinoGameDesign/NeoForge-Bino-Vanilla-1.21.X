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

    // bau que e' um mimico disfarcado (so o servidor sabe: o bau nao manda isso para o cliente)
    public static final Supplier<AttachmentType<Boolean>> MIMIC = ATTACHMENTS.register("mimic",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    // nivel do personagem e pontos: [nivel, forca, vitalidade, agilidade, inteligencia, vontade] (ver PlayerStats)
    public static final Supplier<AttachmentType<java.util.List<Integer>>> CHAR_STATS = ATTACHMENTS.register("char_stats",
            () -> AttachmentType.<java.util.List<Integer>>builder(() -> java.util.List.of(1, 0, 0, 0, 0, 0))
                    .serialize(Codec.INT.listOf()).copyOnDeath().build());

    // sede do jogador (0..20, ver thirst/Thirst): zera so na morte, como a fome; o cansaco acumulado; a dica da agua suja
    public static final Supplier<AttachmentType<Float>> THIRST = ATTACHMENTS.register("thirst",
            () -> AttachmentType.builder(() -> 20f).serialize(Codec.FLOAT).build());
    // reserva escondida da sede (como a saturacao da fome): gasta antes das gotas
    public static final Supplier<AttachmentType<Float>> THIRST_HYD = ATTACHMENTS.register("thirst_hydration",
            () -> AttachmentType.builder(() -> 5f).serialize(Codec.FLOAT).build());
    public static final Supplier<AttachmentType<Float>> THIRST_EXH = ATTACHMENTS.register("thirst_exhaustion",
            () -> AttachmentType.builder(() -> 0f).serialize(Codec.FLOAT).build());
    public static final Supplier<AttachmentType<Boolean>> THIRST_HINT = ATTACHMENTS.register("thirst_hint",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }
}
