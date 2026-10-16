package com.bino.binosvanillaplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ToolUtil {

    // Procura blocos conectados ao bloco inicial que passem no teste "matches".
    // limit = máximo de blocos na lista, contando o inicial.
    public static List<BlockPos> findConnected(Level level, BlockPos start,
                                               Predicate<BlockState> matches, int limit) {
        List<BlockPos> found = new ArrayList<>();      // resultado final
        Set<BlockPos> visited = new HashSet<>();       // posições já examinadas (evita repetir)
        Deque<BlockPos> queue = new ArrayDeque<>();    // fila de posições a examinar

        queue.add(start);
        visited.add(start);

        // Enquanto houver posições na fila e o limite não for atingido
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos current = queue.poll();           // pega a próxima da fila
            found.add(current);                        // essa posição entra no resultado

            // Examina os 26 vizinhos (inclui diagonais)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;   // pula o próprio bloco

                        BlockPos next = current.offset(dx, dy, dz);
                        if (!visited.add(next)) continue;              // já visto? pula

                        // Se o vizinho passa no teste, entra na fila para ser examinado
                        if (matches.test(level.getBlockState(next))) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }
}