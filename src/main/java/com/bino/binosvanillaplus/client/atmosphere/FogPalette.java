package com.bino.binosvanillaplus.client.atmosphere;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

// Paleta propria de neblina por bioma (valores escolhidos a mao para o Bino's Vanilla+).
// Direcao de arte: ar limpo e azulado nos biomas temperados; poeira quente em deserto/savana/badlands; umidade
// esverdeada na selva e no pantano; rosa no cherry; lilas no cogumelo; branco-azulado no frio; e nas cavernas
// uma escuridao com a cor do lugar (verde nas lush, marrom nas dripstone, verde-petroleo no deep dark).
// Bioma de outro mod: cor montada a partir da cor de neblina dele + temperatura/chuva (fallback()).
public final class FogPalette {
    private FogPalette() {}

    private static final Map<ResourceLocation, FogLook> BIOMES = new HashMap<>();
    private static final Map<Biome, FogLook> FALLBACK = new IdentityHashMap<>();

    // caverna comum (qualquer bioma de superficie, mas debaixo da terra)
    public static final FogLook CAVE = FogLook.of(0x0C0E13).night(0x0C0E13).mist(0x15181E)
            .haze(0.0105f).low(0f, 1f).start(6f).build();
    // bem fundo (abaixo do Y 0, deepslate): um pouco mais frio e fechado
    public static final FogLook DEEP_CAVE = FogLook.of(0x080A10).night(0x080A10).mist(0x10131A)
            .haze(0.0125f).low(0f, 1f).start(5f).build();
    // padrao (bioma desconhecido em dimensao com ceu)
    public static final FogLook DEFAULT = FogLook.of(0xA4C8EA).build();

    private static void put(String id, FogLook look) {
        BIOMES.put(ResourceLocation.withDefaultNamespace(id), look);
    }

    static {
        // ---------------------------------------------------------------- temperados
        put("plains", FogLook.of(0x9EC9F0).mist(0xDCEBF5).haze(0.0015f).low(0.0035f, 9f).build());
        put("sunflower_plains", FogLook.of(0xA8D4F2).mist(0xF2EFD6).haze(0.0014f).low(0.0032f, 9f).build());
        put("meadow", FogLook.of(0x9FD3F5).mist(0xF0F2D8).haze(0.0012f).low(0.0040f, 10f).base(-4f).build());
        put("forest", FogLook.of(0x9CCFE8).mist(0xD5ECE3).haze(0.0017f).low(0.0045f, 10f).build());
        put("flower_forest", FogLook.of(0xA8DCEB).mist(0xE6F2DC).haze(0.0016f).low(0.0042f, 10f).build());
        put("birch_forest", FogLook.of(0xA6D8EE).mist(0xE0F2E6).haze(0.0016f).low(0.0040f, 10f).build());
        put("old_growth_birch_forest", FogLook.of(0xA9DBEE).mist(0xE2F3E4).haze(0.0017f).low(0.0048f, 11f).build());
        put("dark_forest", FogLook.of(0x8FA8BE).night(0x080C14).mist(0xB8C8C8).haze(0.0024f).low(0.0080f, 10f).build());
        put("pale_garden", FogLook.of(0xA5AEB3).night(0x22272D).mist(0xC4CACD).sky(0xC5CBD3, 0.8f)
                .haze(0.0045f).low(0.0160f, 14f).start(2f).build());
        put("taiga", FogLook.of(0x9CBFE0).mist(0xD2E2EE).haze(0.0019f).low(0.0055f, 11f).build());
        put("old_growth_pine_taiga", FogLook.of(0x98B8D8).mist(0xCCDCE8).haze(0.0021f).low(0.0065f, 12f).build());
        put("old_growth_spruce_taiga", FogLook.of(0x95B5D6).mist(0xCAD9E6).haze(0.0021f).low(0.0065f, 12f).build());
        put("river", FogLook.of(0x9CCBEF).mist(0xDCEAF4).haze(0.0016f).low(0.0060f, 6f).base(-1f).build());
        put("beach", FogLook.of(0xA8D2F0).mist(0xF2EAD0).haze(0.0016f).low(0.0035f, 7f).build());
        put("stony_shore", FogLook.of(0xA4C6E4).mist(0xDDE7EF).haze(0.0017f).low(0.0040f, 7f).build());
        put("mushroom_fields", FogLook.of(0xB8A0E8).night(0x1A0E2E).mist(0xE0C8F0).sky(0x9C88F0, 0.5f)
                .haze(0.0020f).low(0.0060f, 9f).build());
        put("cherry_grove", FogLook.of(0xF0B8C8).night(0x1A0D18).mist(0xFAD8E2).sky(0xC2B5F0, 0.55f)
                .haze(0.0018f).low(0.0050f, 12f).base(-4f).build());

        // ---------------------------------------------------------------- quentes e secos
        FogLook desert = FogLook.of(0xE9C48E).night(0x101626).mist(0xF3D9A8).sky(0x8CCBEF, 0.45f)
                .haze(0.0022f).low(0.0015f, 22f).build();
        put("desert", desert);
        FogLook savanna = FogLook.of(0xE2BD84).mist(0xF2DCA4).sky(0x86C6EC, 0.35f).haze(0.0019f).low(0.0020f, 16f).build();
        put("savanna", savanna);
        put("savanna_plateau", savanna);
        put("windswept_savanna", savanna);
        FogLook badlands = FogLook.of(0xE8A060).night(0x140E14).mist(0xF2C08A).sky(0x7A9BEA, 0.45f)
                .haze(0.0021f).low(0.0016f, 20f).build();
        put("badlands", badlands);
        put("eroded_badlands", FogLook.of(0xEDAA68).night(0x140E14).mist(0xF4C894).sky(0x7A9BEA, 0.45f)
                .haze(0.0021f).low(0.0016f, 20f).build());
        put("wooded_badlands", FogLook.of(0xDC9A62).night(0x120E12).mist(0xEEC08E).sky(0x8AA6EC, 0.4f)
                .haze(0.0020f).low(0.0020f, 18f).build());

        // ---------------------------------------------------------------- umidos
        FogLook jungle = FogLook.of(0x8FD3A0).night(0x0B140E).mist(0xCDEFCF).sky(0x7FD7D0, 0.45f)
                .haze(0.0022f).low(0.0090f, 14f).build();
        put("jungle", jungle);
        put("bamboo_jungle", FogLook.of(0x86D296).night(0x0B140D).mist(0xC4EEC4).sky(0x80D8C4, 0.45f)
                .haze(0.0023f).low(0.0095f, 14f).build());
        put("sparse_jungle", FogLook.of(0x98D6B0).mist(0xD6F0D6).sky(0x88D8E0, 0.35f).haze(0.0019f).low(0.0060f, 12f).build());
        put("swamp", FogLook.of(0x8FA77A).night(0x0E160E).mist(0xB9C9A0).sky(0x9DBB91, 0.55f)
                .haze(0.0030f).low(0.0130f, 7f).start(2f).base(-1f).build());
        put("mangrove_swamp", FogLook.of(0x88AC9A).night(0x0C1612).mist(0xB8CFC0).sky(0x94BCA8, 0.45f)
                .haze(0.0028f).low(0.0120f, 7f).start(2f).base(-1f).build());

        // ---------------------------------------------------------------- frios e montanhas
        FogLook snowy = FogLook.of(0xC8DDF0).night(0x0E1424).mist(0xEEF4FA).haze(0.0022f).low(0.0050f, 12f).build();
        put("snowy_plains", snowy);
        put("snowy_taiga", FogLook.of(0xC0D6EC).night(0x0E1424).mist(0xEAF2F9).haze(0.0023f).low(0.0055f, 12f).build());
        put("ice_spikes", FogLook.of(0xC4E2F5).night(0x0E1628).mist(0xF2F8FC).haze(0.0020f).low(0.0050f, 12f).build());
        put("snowy_beach", FogLook.of(0xC6DCEF).night(0x0E1424).mist(0xEDF3F8).haze(0.0020f).low(0.0040f, 8f).build());
        put("frozen_river", FogLook.of(0xBBD8F0).night(0x0E1424).mist(0xEEF4FA).haze(0.0019f).low(0.0060f, 6f).build());
        FogLook peaks = FogLook.of(0xC0D6EC).night(0x0C1222).mist(0xEAF2F9).haze(0.0013f).low(0.0040f, 12f).base(-12f).build();
        put("grove", peaks);
        put("snowy_slopes", peaks);
        put("frozen_peaks", peaks);
        put("jagged_peaks", FogLook.of(0xC6DCF0).night(0x0C1222).mist(0xEEF4FA).haze(0.0012f).low(0.0040f, 12f).base(-14f).build());
        FogLook stone = FogLook.of(0xA8C2DC).mist(0xDCE6EE).haze(0.0013f).low(0.0035f, 12f).base(-10f).build();
        put("stony_peaks", stone);
        put("windswept_hills", stone);
        put("windswept_gravelly_hills", stone);
        put("windswept_forest", FogLook.of(0xA2C6DE).mist(0xD8E8EC).haze(0.0015f).low(0.0042f, 12f).base(-8f).build());

        // ---------------------------------------------------------------- oceanos
        put("ocean", FogLook.of(0x95BEEB).mist(0xD2E3F2).haze(0.0020f).low(0.0045f, 6f).base(0f).build());
        put("deep_ocean", FogLook.of(0x86B0E6).mist(0xC8DCF0).haze(0.0021f).low(0.0048f, 6f).base(0f).build());
        put("lukewarm_ocean", FogLook.of(0x98CCEE).mist(0xD8EEF0).haze(0.0019f).low(0.0042f, 6f).base(0f).build());
        put("deep_lukewarm_ocean", FogLook.of(0x8EC2EA).mist(0xD2EAF0).haze(0.0020f).low(0.0045f, 6f).base(0f).build());
        put("warm_ocean", FogLook.of(0xA6DCE8).mist(0xE2F4EA).haze(0.0018f).low(0.0040f, 6f).base(0f).build());
        put("cold_ocean", FogLook.of(0xA8C4E4).mist(0xE0EAF4).haze(0.0021f).low(0.0050f, 6f).base(0f).build());
        put("deep_cold_ocean", FogLook.of(0x9AB8DE).mist(0xD8E4F2).haze(0.0022f).low(0.0052f, 6f).base(0f).build());
        put("frozen_ocean", FogLook.of(0xB8D2EC).night(0x0E1424).mist(0xEAF2F9).haze(0.0022f).low(0.0055f, 6f).base(0f).build());
        put("deep_frozen_ocean", FogLook.of(0xB0CCE8).night(0x0E1424).mist(0xE6F0F8).haze(0.0023f).low(0.0058f, 6f).base(0f).build());

        // ---------------------------------------------------------------- biomas de caverna (so valem debaixo da terra)
        put("lush_caves", FogLook.of(0x16301B).night(0x16301B).mist(0x3A6A36).haze(0.0075f).low(0.0090f, 4f).start(4f).build());
        put("dripstone_caves", FogLook.of(0x241A10).night(0x241A10).mist(0x4A3622).haze(0.0090f).low(0.0050f, 5f).start(5f).build());
        put("deep_dark", FogLook.of(0x031012).night(0x031012).mist(0x08262A).haze(0.0160f).low(0.0060f, 5f).start(3f).build());

        // ---------------------------------------------------------------- Nether (nevoa baixa = bafo do mar de lava)
        put("nether_wastes", FogLook.of(0x4A0C08).night(0x4A0C08).mist(0x8A2A10).haze(0.0045f).low(0.0060f, 10f).absBase(33f).start(3f).build());
        put("crimson_forest", FogLook.of(0x3E0806).night(0x3E0806).mist(0x7A1610).haze(0.0050f).low(0.0060f, 10f).absBase(33f).start(3f).build());
        put("warped_forest", FogLook.of(0x1C0E26).night(0x1C0E26).mist(0x1E4A4A).haze(0.0048f).low(0.0060f, 10f).absBase(33f).start(3f).build());
        put("soul_sand_valley", FogLook.of(0x0E2A28).night(0x0E2A28).mist(0x2E5A56).haze(0.0050f).low(0.0070f, 12f).absBase(33f).start(3f).build());
        put("basalt_deltas", FogLook.of(0x3A3A40).night(0x3A3A40).mist(0x6A6870).haze(0.0065f).low(0.0070f, 12f).absBase(33f).start(2f).build());

        // ---------------------------------------------------------------- End (mar de nevoa roxa debaixo das ilhas)
        FogLook end = FogLook.of(0x120A1A).night(0x120A1A).mist(0x2A1838).haze(0.0016f).low(0.0060f, 14f).absBase(40f).start(8f).build();
        put("the_end", end);
        put("end_highlands", end);
        put("end_midlands", end);
        put("end_barrens", end);
        put("small_end_islands", end);
        put("the_void", FogLook.of(0x0A0A12).night(0x05050A).mist(0x1A1A24).haze(0.0020f).low(0f, 1f).build());
    }

    // aparencia da superficie para esse bioma
    public static FogLook surface(Holder<Biome> holder) {
        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
        FogLook l = id == null ? null : BIOMES.get(id);
        if (l != null && !isCaveBiome(id)) return l;
        return fallback(holder.value(), false);
    }

    // aparencia debaixo da terra: bioma de caverna proprio ou a caverna comum (mais fechada quando funda)
    public static FogLook cave(Holder<Biome> holder, double y) {
        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id != null && isCaveBiome(id)) return BIOMES.get(id);
        return y < 0 ? DEEP_CAVE : CAVE;
    }

    // dimensao com teto (Nether e parecidos) ou sem luz do ceu (End): a aparencia vem direto do bioma
    public static FogLook dimension(Holder<Biome> holder) {
        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
        FogLook l = id == null ? null : BIOMES.get(id);
        return l != null ? l : fallback(holder.value(), true);
    }

    private static boolean isCaveBiome(ResourceLocation id) {
        if (!"minecraft".equals(id.getNamespace())) return false;
        String p = id.getPath();
        return p.equals("lush_caves") || p.equals("dripstone_caves") || p.equals("deep_dark");
    }

    // bioma de outro mod: parte da cor de neblina dele e puxa para quente/frio/umido pela temperatura e chuva
    private static FogLook fallback(Biome b, boolean noSky) {
        return FALLBACK.computeIfAbsent(b, k -> {
            int base = k.getFogColor() & 0xFFFFFF;
            if (noSky) {
                return FogLook.of(base).night(base).mist(FogLook.mixRgb(base, 0xFFFFFF, 0.2f)).haze(0.0040f)
                        .low(0.0040f, 10f).absBase(33f).start(3f).build();
            }
            float t = k.getBaseTemperature();
            boolean wet = k.hasPrecipitation();
            int day = FogLook.mixRgb(base, 0x9EC9F0, 0.5f);
            float haze = 0.0016f, low = 0.0040f, h = 10f;
            if (t >= 1.5f && !wet) { day = FogLook.mixRgb(base, 0xE6BE88, 0.65f); haze = 0.0021f; low = 0.0016f; h = 20f; }
            else if (t >= 0.9f && wet) { day = FogLook.mixRgb(base, 0x92D2A4, 0.45f); haze = 0.0021f; low = 0.0080f; h = 13f; }
            else if (t < 0.15f) { day = FogLook.mixRgb(base, 0xC8DDF0, 0.6f); haze = 0.0021f; low = 0.0050f; h = 12f; }
            return FogLook.of(day).haze(haze).low(low, h).build();
        });
    }

    static void clearCache() { FALLBACK.clear(); }
}
