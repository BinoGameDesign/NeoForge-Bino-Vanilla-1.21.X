package com.bino.binosvanillaplus.pocket;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.DimensionalDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Constroi e aumenta a sala de um lote: chao de concreto branco, paredes/teto de "barreira" (invisiveis, so impedem de
// sair) e a porta de saida na parede do fundo. NUNCA apaga o que o jogador construiu: so troca ar/barreira.
public final class PocketRooms {
    private PocketRooms() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;   // sem avisar vizinhos

    // garante sala no tamanho certo e a porta de saida no lugar
    public static void ensure(ServerLevel pocket, PocketData.Plot plot) {
        if (plot.builtLevel != plot.level) build(pocket, plot);
        placeExit(pocket, plot);
    }

    private static void build(ServerLevel pocket, PocketData.Plot plot) {
        int cx = PocketDimension.centerX(plot.index);
        int half = PocketDimension.halfFor(plot.level);
        int height = PocketDimension.heightFor(plot.level);
        int floor = PocketDimension.FLOOR_Y;

        // a porta de saida do tamanho antigo sai (a nova nasce na parede nova)
        if (plot.builtLevel >= 0) {
            BlockPos old = new BlockPos(cx, floor + 1, -PocketDimension.halfFor(plot.builtLevel));
            if (pocket.getBlockState(old).getBlock() instanceof DimensionalDoorBlock) {
                pocket.setBlock(old.above(), Blocks.AIR.defaultBlockState(), FLAGS);
                pocket.setBlock(old, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }

        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockState concrete = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = cx - half - 1; x <= cx + half + 1; x++) {
            for (int z = -half - 1; z <= half + 1; z++) {
                for (int y = floor - 1; y <= floor + height + 1; y++) {
                    p.set(x, y, z);
                    boolean shell = x == cx - half - 1 || x == cx + half + 1 || z == -half - 1 || z == half + 1
                            || y == floor - 1 || y == floor + height + 1;
                    BlockState cur = pocket.getBlockState(p);
                    boolean free = cur.isAir() || cur.is(Blocks.BARRIER);
                    if (shell) {
                        if (free && !cur.is(Blocks.BARRIER)) pocket.setBlock(p, barrier, FLAGS);
                    } else if (y == floor) {
                        if (free) pocket.setBlock(p, concrete, FLAGS);
                    } else if (cur.is(Blocks.BARRIER)) {
                        pocket.setBlock(p, air, FLAGS);   // era parede/teto do tamanho antigo
                    }
                }
            }
        }
        plot.builtLevel = plot.level;
    }

    // porta de saida: na parede do fundo (norte), virada para dentro da sala
    public static void placeExit(ServerLevel pocket, PocketData.Plot plot) {
        BlockPos lower = exitPos(plot);
        BlockState cur = pocket.getBlockState(lower);
        if (cur.getBlock() instanceof DimensionalDoorBlock
                && pocket.getBlockState(lower.above()).getBlock() instanceof DimensionalDoorBlock) return;
        DimensionalDoorBlock.placeAt(pocket, lower, Direction.SOUTH);
    }

    public static BlockPos exitPos(PocketData.Plot plot) {
        return new BlockPos(PocketDimension.centerX(plot.index), PocketDimension.FLOOR_Y + 1,
                -PocketDimension.halfFor(plot.level));
    }
}
