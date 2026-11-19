package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.entity.RatTrapBlockEntity;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.sound.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

// Ratoeira de mola.
//  - Clique com pedaco de queijo: +1 isca (queijo inteiro: +5), ate 16. Agachado com a mao vazia: tira as iscas.
//  - Rato pisa com isca: morre (solta a carne) e gasta 1 isca. Quando a ULTIMA isca acaba, o arco fecha
//    (animacao + som + particulas) e a ratoeira fica "disparada".
//  - Jogador ou outro mob pisa numa ratoeira armada: fecha na hora, 2 coracoes de dano e lentidao.
//  - Disparada: clique com a mao vazia rearma. Emite redstone enquanto disparada.
public class RatTrapBlock extends BaseEntityBlock {
    public static final MapCodec<RatTrapBlock> CODEC = simpleCodec(RatTrapBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SPRUNG = BooleanProperty.create("sprung");
    private static final VoxelShape SHAPE = box(3, 0, 1, 13, 2, 15);
    public static final int SNAP_EVENT = 1;
    public static final float STEP_DAMAGE = 4.0f;

    public RatTrapBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPRUNG, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) { b.add(FACING, SPRUNG); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RatTrapBlockEntity(pos, state); }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP); }
    @Override protected BlockState updateShape(BlockState s, Direction d, BlockState o, net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockPos op) {
        return d == Direction.DOWN && !s.canSurvive(l, p) ? Blocks.AIR.defaultBlockState() : super.updateShape(s, d, o, l, p, op);
    }

    @Nullable private static RatTrapBlockEntity be(Level l, BlockPos p) { return l.getBlockEntity(p) instanceof RatTrapBlockEntity t ? t : null; }

    // ------------------------------------------------------------------ isca
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int add = stack.is(ModItems.CHEESE_SLICE.get()) ? 1 : stack.is(ModItems.CHEESE.get()) ? 5 : 0;
        if (add == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        RatTrapBlockEntity t = be(level, pos);
        if (t == null || t.getBait() >= RatTrapBlockEntity.MAX_BAIT) return ItemInteractionResult.CONSUME;
        if (!level.isClientSide) {
            t.setBait(t.getBait() + add);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7f, 1.3f);
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.rat_trap_bait", t.getBait(), RatTrapBlockEntity.MAX_BAIT), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        RatTrapBlockEntity t = be(level, pos);
        if (t == null) return InteractionResult.PASS;
        if (state.getValue(SPRUNG)) {   // rearmar
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(SPRUNG, false), 3);
                t.refreshTracking();
                level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && t.getBait() > 0) {   // tirar as iscas
            if (!level.isClientSide) {
                ItemStack back = new ItemStack(ModItems.CHEESE_SLICE.get(), t.getBait());
                if (!player.getInventory().add(back)) player.drop(back, false);
                t.setBait(0);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.binosvanillaplus.rat_trap_bait", t.getBait(), RatTrapBlockEntity.MAX_BAIT), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ disparo
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || state.getValue(SPRUNG) || !(e instanceof LivingEntity le) || !le.isAlive()) return;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        RatTrapBlockEntity t = be(level, pos);
        if (t == null) return;
        if (e instanceof RatEntity rat && !(e instanceof com.bino.binosvanillaplus.entity.rat.DireRatEntity)) {   // o Rato Gigante so pisa (como os outros mobs)
            if (t.getBait() <= 0) { snap(level, pos, state); return; }
            rat.hurt(level.damageSources().generic(), 100f);   // pego: solta a carne
            level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.8f, 1.6f);
            t.setBait(t.getBait() - 1);
            if (t.getBait() == 0) snap(level, pos, state);   // acabou o queijo: fecha
        } else {
            le.hurt(level.damageSources().generic(), STEP_DAMAGE);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            snap(level, pos, state);
        }
    }

    public static void snap(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(SPRUNG, true), 3);
        level.blockEvent(pos, state.getBlock(), SNAP_EVENT, 0);   // cliente anima o arco
        level.playSound(null, pos, ModSounds.RAT_TRAP_SNAP.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 10, 0.2, 0.05, 0.2, 0.15);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 8, 0.2, 0.02, 0.3, 0.05);
        }
        if (level.getBlockEntity(pos) instanceof RatTrapBlockEntity t) t.refreshTracking();
        level.updateNeighborsAt(pos, state.getBlock());
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == SNAP_EVENT) {
            if (level.isClientSide && level.getBlockEntity(pos) instanceof RatTrapBlockEntity t) t.snapStart = level.getGameTime();
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    // redstone enquanto disparada
    @Override protected boolean isSignalSource(BlockState s) { return true; }
    @Override protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) { return s.getValue(SPRUNG) ? 15 : 0; }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RatTrapBlockEntity t && t.getBait() > 0)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, new ItemStack(ModItems.CHEESE_SLICE.get(), t.getBait()));
        super.onRemove(state, level, pos, newState, moving);
    }
}
