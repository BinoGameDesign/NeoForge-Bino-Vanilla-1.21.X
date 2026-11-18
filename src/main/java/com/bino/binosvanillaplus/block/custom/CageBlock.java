package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.entity.CageBlockEntity;
import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

// Gaiola (captura viva).
//  - Aberta: clique com queijo (ratos) ou sementes (galinhas/papagaios) poe a isca. Agachado + mao vazia tira a isca.
//  - O bicho certo entra -> a porta desce (animacao + som) e ele fica preso, aparecendo la dentro.
//  - Fechada com bicho: clique com a mao vazia abre e solta o bicho. Quebrar a gaiola leva o bicho junto (o item guarda).
//  - Fechada vazia: clique com a mao vazia abre de novo. Emite redstone enquanto tem bicho preso.
public class CageBlock extends BaseEntityBlock {
    public static final MapCodec<CageBlock> CODEC = simpleCodec(CageBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final int DOOR_EVENT = 1;
    private static final VoxelShape SHAPE = box(2.8, 0, 0.2, 13.2, 10, 15);
    private static final VoxelShape FLOOR = box(2.8, 0, 0.2, 13.2, 1, 15);

    public CageBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CLOSED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) { b.add(FACING, CLOSED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(CLOSED) ? SHAPE : FLOOR; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CageBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CAGE.get(), CageBlockEntity::serverTick);
    }

    @Nullable private static CageBlockEntity be(Level l, BlockPos p) { return l.getBlockEntity(p) instanceof CageBlockEntity c ? c : null; }

    private static void door(Level level, BlockPos pos, BlockState state, boolean close) {
        level.setBlock(pos, state.setValue(CLOSED, close), 3);
        level.blockEvent(pos, state.getBlock(), DOOR_EVENT, close ? 1 : 0);
        level.playSound(null, pos, close ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.9f, 1.3f);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        CageBlockEntity c = be(level, pos);
        if (c == null || state.getValue(CLOSED) || !c.getBait().isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!CageBlockEntity.isCheese(stack) && !CageBlockEntity.isSeed(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            c.setBait(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        CageBlockEntity c = be(level, pos);
        if (c == null) return InteractionResult.PASS;
        if (state.getValue(CLOSED)) {                                     // abre (e solta o bicho)
            if (!level.isClientSide) {
                c.release(level, pos.relative(state.getValue(FACING)));
                door(level, pos, state, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && !c.getBait().isEmpty()) {          // tira a isca
            if (!level.isClientSide) {
                ItemStack back = c.getBait().copy();
                if (!player.getInventory().add(back)) player.drop(back, false);
                c.setBait(ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // bicho com a isca certa entra: prende
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || state.getValue(CLOSED) || !(e instanceof LivingEntity le) || !le.isAlive()) return;
        CageBlockEntity c = be(level, pos);
        if (c == null || !c.lures(e)) return;
        if (e.getBoundingBox().getCenter().distanceToSqr(pos.getX() + 0.5, e.getY(), pos.getZ() + 0.5) > 0.12) return;   // tem que estar bem dentro
        c.capture(le);
        door(level, pos, level.getBlockState(pos), true);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == DOOR_EVENT) {
            if (level.isClientSide && level.getBlockEntity(pos) instanceof CageBlockEntity c) c.doorStart = level.getGameTime();
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Override protected boolean isSignalSource(BlockState s) { return true; }
    @Override protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) {
        return l.getBlockEntity(p) instanceof CageBlockEntity c && c.hasCreature() ? 15 : 0;
    }

    // quebrar: o item leva a isca e o bicho (dados do bloco); colocar de novo restaura (o jogo le BLOCK_ENTITY_DATA)
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && be(level, pos) instanceof CageBlockEntity c && !player.isCreative()) {
            ItemStack item = new ItemStack(this);
            if (c.hasCreature() || !c.getBait().isEmpty()) {
                item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(c.saveWithId(level.registryAccess())));
                if (c.hasCreature()) item.set(DataComponents.CUSTOM_NAME, Component.translatable("block.binosvanillaplus.cage.with",
                        net.minecraft.world.entity.EntityType.by(c.getCreature()).map(t -> t.getDescription()).orElse(Component.literal("?"))));
            }
            popResource(level, pos, item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && be(level, pos) instanceof CageBlockEntity c && c.hasCreature()) level.setBlock(pos, state.setValue(CLOSED, true), 3);
    }
}
