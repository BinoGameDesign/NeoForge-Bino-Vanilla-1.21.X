package com.bino.binosvanillaplus.pocket;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.DimensionalDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Constroi e aumenta a sala de um lote. Chao, paredes e teto sao a "Parede Dimensional" (cimento branco inquebravel).
// O chao fica UM bloco abaixo do primeiro bloco livre (FLOOR_Y - 1), entao a camada FLOOR_Y esta vazia para o jogador
// montar o proprio chao. A porta de saida fica na parede do fundo. NUNCA apaga o que o jogador construiu.
public final class PocketRooms {
    private PocketRooms() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;   // sem avisar vizinhos
    private static final int FORMAT = 1;

    // garante sala no tamanho certo e a porta de saida no lugar
    // devolve true se construiu/ampliou (o chamador deve marcar os dados como alterados)
    public static boolean ensure(ServerLevel pocket, PocketData.Plot plot) {
        boolean built = plot.builtLevel != plot.level || plot.fmt < FORMAT;
        if (built) build(pocket, plot);
        placeExit(pocket, plot);
        return built;
    }

    private static void build(ServerLevel pocket, PocketData.Plot plot) {
        int cx = PocketDimension.centerX(plot.index);
        int half = PocketDimension.halfFor(plot.level);
        int height = PocketDimension.heightFor(plot.level);
        int floor = PocketDimension.FLOOR_Y;

        // a porta de saida do tamanho/formato antigo sai (a nova nasce na parede nova)
        if (plot.builtLevel >= 0) {
            int oldY = plot.fmt < 1 ? floor + 1 : floor;
            BlockPos old = new BlockPos(cx, oldY, -PocketDimension.halfFor(plot.builtLevel));
            if (pocket.getBlockState(old).getBlock() instanceof DimensionalDoorBlock) {
                pocket.setBlock(old.above(), Blocks.AIR.defaultBlockState(), FLAGS);
                pocket.setBlock(old, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }

        BlockState wall = ModBlocks.POCKET_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = cx - half - 1; x <= cx + half + 1; x++) {
            for (int z = -half - 1; z <= half + 1; z++) {
                for (int y = floor - 1; y <= floor + height + 1; y++) {
                    p.set(x, y, z);
                    boolean shell = x == cx - half - 1 || x == cx + half + 1 || z == -half - 1 || z == half + 1
                            || y == floor - 1 || y == floor + height + 1;
                    BlockState cur = pocket.getBlockState(p);
                    if (shell) {
                        // so ocupa ar ou o material antigo (barreira); nunca destroi construcao do jogador
                        if (cur.isAir() || cur.is(Blocks.BARRIER)) pocket.setBlock(p, wall, FLAGS);
                    } else if (cur.is(Blocks.BARRIER)) {
                        pocket.setBlock(p, air, FLAGS);                    // era parede/teto do tamanho antigo
                    } else if (plot.fmt < 1 && y == floor && cur.is(Blocks.WHITE_CONCRETE)) {
                        pocket.setBlock(p, air, FLAGS);                    // chao do formato antigo
                    } else if (cur.is(ModBlocks.POCKET_WALL.get())) {
                        pocket.setBlock(p, air, FLAGS);                    // parede do tamanho antigo (agora e interior)
                    }
                }
            }
        }
        plot.builtLevel = plot.level;
        plot.fmt = FORMAT;
    }

    // porta de saida: na parede do fundo (norte), em cima do chao, virada para dentro da sala
    public static void placeExit(ServerLevel pocket, PocketData.Plot plot) {
        BlockPos lower = exitPos(plot);
        BlockState cur = pocket.getBlockState(lower);
        if (cur.getBlock() instanceof DimensionalDoorBlock
                && pocket.getBlockState(lower.above()).getBlock() instanceof DimensionalDoorBlock) return;
        DimensionalDoorBlock.placeAt(pocket, lower, Direction.SOUTH);
    }

    public static BlockPos exitPos(PocketData.Plot plot) {
        return new BlockPos(PocketDimension.centerX(plot.index), PocketDimension.FLOOR_Y,
                -PocketDimension.halfFor(plot.level));
    }
}
