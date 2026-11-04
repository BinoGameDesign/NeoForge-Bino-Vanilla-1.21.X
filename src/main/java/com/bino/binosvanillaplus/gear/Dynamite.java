package com.bino.binosvanillaplus.gear;

import com.bino.binosvanillaplus.block.custom.DynamiteBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.*;

// Explosao da dinamite, feita para minerar: quebra os blocos comuns num raio de ~3 e SOLTA todos eles (pedra vira
// pedregulho como numa picareta), mas nunca quebra minerios, blocos com conteudo (bau, fornalha...), liquidos nem
// blocos inquebraveis. O dano e o empurrao nas criaturas sao os da explosao normal do jogo (sem quebrar nada).
public final class Dynamite {
    private Dynamite() {}

    private static final float RADIUS = 3.2f;
    private static final Map<UUID, Set<GlobalPos>> OWNED = new HashMap<>();

    public static void track(UUID owner, ServerLevel level, BlockPos pos) {
        OWNED.computeIfAbsent(owner, k -> new HashSet<>()).add(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    public static void untrack(ServerLevel level, BlockPos pos) {
        GlobalPos g = GlobalPos.of(level.dimension(), pos);
        for (Set<GlobalPos> s : OWNED.values()) s.remove(g);
    }

    // dinamites do jogador nesta dimensao, ate 'range' blocos, da mais perto para a mais longe
    public static List<BlockPos> ownedNear(UUID owner, ServerLevel level, BlockPos from, double range) {
        List<BlockPos> out = new ArrayList<>();
        for (GlobalPos g : OWNED.getOrDefault(owner, Set.of())) {
            if (g.dimension() != level.dimension() || g.pos().distSqr(from) > range * range) continue;
            if (level.isLoaded(g.pos()) && level.getBlockState(g.pos()).getBlock() instanceof DynamiteBlock) out.add(g.pos());
        }
        out.sort(Comparator.comparingDouble(p -> p.distSqr(from)));
        return out;
    }

    public static boolean isOre(BlockState s) {
        if (s.is(Tags.Blocks.ORES) || s.is(Blocks.ANCIENT_DEBRIS)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("ore_");
    }

    public static void blast(ServerLevel level, BlockPos c) {
        if (level.getBlockState(c).getBlock() instanceof DynamiteBlock) level.removeBlock(c, false);
        untrack(level, c);
        int r = (int) Math.ceil(RADIUS);
        var rnd = level.getRandom();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > RADIUS * RADIUS + rnd.nextFloat() * 2.0f) continue;   // borda irregular (nao fica uma bola perfeita)
                    BlockPos p = c.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (s.getBlock() instanceof DynamiteBlock) { DynamiteBlock.prime(level, p, 3 + rnd.nextInt(5)); continue; }   // em cadeia
                    if (s.getBlock() instanceof LiquidBlock || s.hasBlockEntity() || isOre(s)) continue;
                    if (s.getDestroySpeed(level, p) < 0) continue;
                    level.destroyBlock(p, true);
                }
            }
        }
        level.explode(null, c.getX() + 0.5, c.getY() + 0.4, c.getZ() + 0.5, 2.6f, Level.ExplosionInteraction.NONE);
    }
}
