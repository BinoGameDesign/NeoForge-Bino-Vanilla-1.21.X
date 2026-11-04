package com.bino.binosvanillaplus.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

// Cranio fossilizado (frente com orbitas e dentes)
public class FossilSkullBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FossilSkullBlock> CODEC = simpleCodec(FossilSkullBlock::new);

    public FossilSkullBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(FossilBoneBlock.SPECIES, 0).setValue(FossilBoneBlock.NATURAL, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FossilBoneBlock.SPECIES, FossilBoneBlock.NATURAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
