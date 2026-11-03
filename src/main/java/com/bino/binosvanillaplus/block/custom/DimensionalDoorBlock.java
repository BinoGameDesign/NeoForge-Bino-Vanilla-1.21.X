package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.pocket.PocketDimension;
import com.bino.binosvanillaplus.pocket.PocketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

// Porta Dimensional: uma porta de 2 blocos que nunca abre de verdade. Clicar nela leva (ou pede para levar) a
// dimensao do dono. Fora do mundo comum, na sala, a mesma porta e a SAIDA. Nao dropa nada (a chave cria outra).
// Quem manda e o PocketManager: a porta de fora e identificada pela posicao guardada nos dados do lote.
public class DimensionalDoorBlock extends DoorBlock {
    public DimensionalDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);   // IRON = nao abre com a mao nem com redstone/vento
    }

    // cria as duas metades. 'facing' = lado para onde a frente da porta olha
    public static void placeAt(Level level, BlockPos lower, Direction facing) {
        BlockState base = ModBlocks.DIMENSIONAL_DOOR.get().defaultBlockState()
                .setValue(FACING, facing).setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false).setValue(POWERED, false);
        level.setBlock(lower, base.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        level.setBlock(lower.above(), base.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            PocketManager.useDoor(sp, sl, lower, level.getBlockState(lower));
        }
        return InteractionResult.CONSUME;
    }

    // a porta de saida (dentro da dimensao) nao quebra; a de fora quebra normalmente
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level instanceof Level l && PocketDimension.is(l)) return 0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    // redstone nao faz nada
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
    }

    // porta quebrada/removida (por qualquer causa): quem estava la dentro e mandado de volta
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER && level instanceof ServerLevel sl) {
            PocketManager.onDoorRemoved(sl, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
