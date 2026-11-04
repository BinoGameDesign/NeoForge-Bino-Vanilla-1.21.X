package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.entity.CameraTripodBlockEntity;
import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import com.bino.binosvanillaplus.item.custom.CameraItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Tripe: coloque a camera nele (clique com a camera) e clique de novo para o timer de 5 s. A foto sai do ponto de
// vista do tripe, olhando para onde voce olhava ao coloca-lo. Agachado + clique tira a camera.
public class CameraTripodBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CameraTripodBlock> CODEC = simpleCodec(CameraTripodBlock::new);
    public static final BooleanProperty HAS_CAMERA = BooleanProperty.create("has_camera");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);

    public CameraTripodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(HAS_CAMERA, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, HAS_CAMERA); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());   // a camera olha para onde o jogador olhava
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof CameraItem) || state.getValue(HAS_CAMERA)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CameraTripodBlockEntity be) {
            be.setCamera(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.setBlock(pos, state.setValue(HAS_CAMERA, true), 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(HAS_CAMERA)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CameraTripodBlockEntity be)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {   // tira a camera
            ItemStack cam = be.getCamera();
            be.setCamera(ItemStack.EMPTY);
            level.setBlock(pos, state.setValue(HAS_CAMERA, false), 3);
            if (!player.getInventory().add(cam)) player.drop(cam, false);
            return InteractionResult.CONSUME;
        }
        if (!be.counting() && player instanceof ServerPlayer sp) {
            be.start(sp);
            sp.displayClientMessage(Component.translatable("message.binosvanillaplus.tripod.timer").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CameraTripodBlockEntity be && !be.getCamera().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getCamera());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CameraTripodBlockEntity(pos, state); }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CAMERA_TRIPOD.get()) return null;
        return (lvl, pos, st, be) -> CameraTripodBlockEntity.serverTick(lvl, pos, st, (CameraTripodBlockEntity) be);
    }
}
