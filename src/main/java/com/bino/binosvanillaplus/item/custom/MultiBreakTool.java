package com.bino.binosvanillaplus.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface MultiBreakTool {

    // Devolve as posições dos blocos EXTRAS que devem quebrar junto com o bloco original.
    // player = quem está minerando | origin = posição do bloco quebrado | state = tipo do bloco quebrado
    List<BlockPos> getExtraBlocks(ServerPlayer player, BlockPos origin, BlockState state);
}