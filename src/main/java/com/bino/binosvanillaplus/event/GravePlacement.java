package com.bino.binosvanillaplus.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

// Acha onde pode ficar o tumulo: perto de onde o jogador morreu, em chao firme, sem lava nem agua.
// Se morreu no vazio ou em lava, vai para o bloco seguro mais proximo; senao, para a superficie.
final class GravePlacement {
    private GravePlacement() {}

    static BlockPos find(ServerLevel level, BlockPos origin) {
        // 1) perto de onde morreu (mesmo nivel, +-6 blocos de altura), crescendo em volta
        for (int r = 0; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) continue;
                    for (int i = 0; i <= 12; i++) {
                        int dy = (i + 1) / 2 * (i % 2 == 1 ? 1 : -1);   // 0, +1, -1, +2, -2...
                        BlockPos p = new BlockPos(x, origin.getY() + dy, z);
                        if (valid(level, p)) return p;
                    }
                }
            }
        }
        // 2) superficie em volta (para quem caiu no vazio ou morreu fundo demais)
        for (int r = 0; r <= 64; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos p = surface(level, origin.getX() + dx, origin.getZ() + dz);
                    if (p != null) return p;
                }
            }
        }
        // 3) ultimo recurso: o ponto de spawn do mundo
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos p = surface(level, spawn.getX(), spawn.getZ());
        if (p != null) return p;
        return new BlockPos(spawn.getX(), Math.max(level.getMinBuildHeight() + 2, spawn.getY()), spawn.getZ());
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);   // primeiro bloco livre em cima do chao
        BlockPos p = new BlockPos(x, y, z);
        return valid(level, p) ? p : null;
    }

    // bloco livre, com chao firme embaixo, espaco em cima para o allay e sem liquido por perto
    private static boolean valid(ServerLevel level, BlockPos p) {
        if (p.getY() <= level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight() - 3) return false;
        BlockState here = level.getBlockState(p);
        if (!here.canBeReplaced() || !here.getFluidState().isEmpty()) return false;
        BlockPos below = p.below();
        BlockState b = level.getBlockState(below);
        if (!b.isFaceSturdy(level, below, Direction.UP) || !b.getFluidState().isEmpty()) return false;
        if (level.getBlockState(p.above()).blocksMotion() || level.getBlockState(p.above(2)).blocksMotion()) return false;
        for (Direction d : Direction.values()) {   // nada de lava ao redor
            if (level.getFluidState(p.relative(d)).is(FluidTags.LAVA)) return false;
        }
        return true;
    }
}
