package com.bino.binosmining.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

public class ToolUtil {

    // Procura blocos conectados ao bloco inicial que passem no teste "matches".
    // limit = máximo de blocos na lista, contando o inicial.
    // Otimizado: posições vistas guardadas como long (sem um BlockPos por vizinho), um BlockPos mutável para ler os
    // estados, e nunca olha chunk descarregado (getBlockState num chunk fora da área carregada poderia carregá-lo).
    public static List<BlockPos> findConnected(Level level, BlockPos start,
                                               Predicate<BlockState> matches, int limit) {
        List<BlockPos> found = new ArrayList<>();                  // resultado final
        LongOpenHashSet visited = new LongOpenHashSet();           // posições já examinadas (evita repetir)
        Deque<BlockPos> queue = new ArrayDeque<>();                // fila de posições a examinar
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        queue.add(start);
        visited.add(start.asLong());

        // Enquanto houver posições na fila e o limite não for atingido
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos current = queue.poll();           // pega a próxima da fila
            found.add(current);                        // essa posição entra no resultado

            // Examina os 26 vizinhos (inclui diagonais)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;   // pula o próprio bloco

                        cursor.setWithOffset(current, dx, dy, dz);
                        if (!visited.add(cursor.asLong())) continue;   // já visto? pula
                        if (!level.isLoaded(cursor)) continue;

                        // Se o vizinho passa no teste, entra na fila para ser examinado
                        if (matches.test(level.getBlockState(cursor))) {
                            queue.add(cursor.immutable());
                        }
                    }
                }
            }
        }
        return found;
    }
}
