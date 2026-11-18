package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Queijo inteiro colocado no chao: clique direito tira um pedaco (item "pedaco de queijo"). 5 pedacos por queijo.
// Largado por 3 dias atrai ratos (tag binosvanillaplus:rat_food).
public class CheeseBlock extends Block {
    public static final MapCodec<CheeseBlock> CODEC = simpleCodec(CheeseBlock::new);
    public static final int MAX_BITES = 4;   // 0 = inteiro; 4 = so resta 1 pedaco
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, MAX_BITES);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 6, 14);

    public CheeseBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BITES, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BITES); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack slice = new ItemStack(ModItems.CHEESE_SLICE.get());
            if (!player.getInventory().add(slice)) player.drop(slice, false);
            takeBite(level, pos, state);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8f, 1.4f);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // tira um pedaco (tambem usado pelos ratos). O ultimo pedaco some.
    public static void takeBite(Level level, BlockPos pos, BlockState state) {
        int b = state.getValue(BITES);
        if (b < MAX_BITES) level.setBlock(pos, state.setValue(BITES, b + 1), 3);
        else level.removeBlock(pos, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return dir == Direction.DOWN && !state.canSurvive(level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, other, level, pos, otherPos);
    }
}
