package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.entity.GraveBlockEntity;
import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Tumulo do jogador morto (modelo grave.json). Inquebravel para quem nao e creative: so some ao pegar tudo ou dissipar.
public class GraveBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    // caixa do modelo virado para o norte (lapide + base + caveira na frente)
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        VoxelShape north = Block.box(3.3, 0, 4.5, 12.7, 13, 9.4);
        VoxelShape s = north;
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < 4; i++) {
            SHAPES[order[i].get2DDataValue()] = s;
            s = rotate(s);
        }
    }

    // gira 90 graus (horario, visto de cima) em volta do centro do bloco
    private static VoxelShape rotate(VoxelShape shape) {
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                out[0] = Shapes.or(out[0], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
        return out[0];
    }

    public GraveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof GraveBlockEntity be) be.tryOpen(sp);
        return InteractionResult.CONSUME;
    }

    // se for removido por fora (creative, comando): o que estava dentro cai no chao em vez de se perder
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GraveBlockEntity grave) {
                GraveBlockEntity.REGISTRY.remove(grave.getGraveId());
                if (!grave.wasDissipated() && level instanceof ServerLevel sl) {
                    Containers.dropContents(level, pos, grave.getItems());
                    if (grave.getExperience() > 0) ExperienceOrb.award(sl, pos.getCenter(), grave.getExperience());
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.GRAVE.get()) return null;
        return (lvl, pos, st, be) -> GraveBlockEntity.serverTick(lvl, pos, st, (GraveBlockEntity) be);
    }
}
