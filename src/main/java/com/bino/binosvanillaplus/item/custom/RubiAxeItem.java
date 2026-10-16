package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.util.ToolUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RubiAxeItem extends AxeItem implements MultiBreakTool {

    private static final int MAX_BLOCKS = 64;   // limite de troncos

    public RubiAxeItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public List<BlockPos> getExtraBlocks(ServerPlayer player, BlockPos origin, BlockState state) {
        // Só age se o bloco quebrado for um tronco (tag minecraft:logs)
        if (!state.is(BlockTags.LOGS)) return List.of();

        // Acha todos os troncos conectados, até 64
        return ToolUtil.findConnected(player.level(), origin,
                s -> s.is(BlockTags.LOGS),
                MAX_BLOCKS);
    }
}