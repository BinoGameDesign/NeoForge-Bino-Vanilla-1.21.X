package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.pocket.PocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

// Nivel da regiao (sem a variacao de cada mob). Mesma conta no servidor e no cliente (mapa), sem depender do jogador:
//   1) anel: distancia ate o spawn do mundo (+5 niveis a cada 500 blocos)
//   2) aspereza do bioma (-5 a +15), media dos biomas em volta (borda suave entre biomas)
//   3) profundidade: da superficie ao fundo do mundo soma ate +4 (deep dark +5)
// Nether: 40 + distancia/60 (ate 80). End: 75 + distancia/200 (ate 100). Dimensao Portatil: sem nivel.
public final class RegionDifficulty {
    private RegionDifficulty() {}

    public static final double BLOCKS_PER_LEVEL = 100.0;   // 500 blocos = 5 niveis
    public static final int MIN = 1, MAX = 100;

    private static TagKey<Biome> tag(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "difficulty/" + path));
    }

    private static final TagKey<Biome> MINUS_5 = tag("minus_5"), PLUS_5 = tag("plus_5"), PLUS_10 = tag("plus_10"), PLUS_15 = tag("plus_15");

    public static int biomeOffset(Holder<Biome> b) {
        if (b.is(PLUS_15)) return 15;
        if (b.is(PLUS_10)) return 10;
        if (b.is(PLUS_5)) return 5;
        if (b.is(MINUS_5)) return -5;
        return 0;
    }

    // nivel da superficie naquela coluna (anel + bioma suavizado); -1 = lugar sem nivel
    public static double surfaceLevel(Level level, int x, int z) {
        if (level.dimension().equals(PocketDimension.LEVEL)) return -1;
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = x - spawn.getX(), dz = z - spawn.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (level.dimension().equals(Level.NETHER)) return clamp(40 + dist / 60.0 + smoothBiome(level, x, z, 64) * 0.5, 40, 80);
        if (level.dimension().equals(Level.END)) return clamp(75 + dist / 200.0, 75, 100);
        return clamp(1 + dist / BLOCKS_PER_LEVEL + smoothBiome(level, x, z, level.getSeaLevel()), MIN, MAX);
    }

    // aspereza media de 9 pontos em volta (grade de 24 blocos)
    private static double smoothBiome(Level level, int x, int z, int fallbackY) {
        int sum = 0, n = 0;
        for (int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                int sx = x + i * 24, sz = z + k * 24;
                int y = level.hasChunkAt(new BlockPos(sx, 0, sz)) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, sx, sz) - 1 : fallbackY;
                sum += biomeOffset(level.getBiome(new BlockPos(sx, y, sz)));
                n++;
            }
        }
        return sum / (double) n;
    }

    // nivel do lugar (superficie + profundidade)
    public static double levelAt(Level level, BlockPos pos) {
        double s = surfaceLevel(level, pos.getX(), pos.getZ());
        if (s < 0) return -1;
        if (!level.dimension().equals(Level.OVERWORLD)) return s;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        int bottom = level.getMinBuildHeight();
        double depth = clamp((surface - pos.getY()) / (double) Math.max(16, surface - bottom), 0, 1);
        double bonus = 4 * depth;
        if (level.getBiome(pos).is(Biomes.DEEP_DARK)) bonus += 5;
        return clamp(s + bonus, MIN, MAX);
    }

    // faixa de 5 niveis: 1 = 1-5, 2 = 6-10 ...
    public static int band(double level) { return (int) Math.floor((Math.max(1, level) - 1) / 5.0) + 1; }

    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
}
