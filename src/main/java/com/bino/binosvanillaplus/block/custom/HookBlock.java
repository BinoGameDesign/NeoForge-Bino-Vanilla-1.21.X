package com.bino.binosvanillaplus.block.custom;

import com.mojang.serialization.MapCodec;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

// Gancho lancado: fica preso no chao, numa parede ou no teto, com uma corda de 10 blocos pendurada.
// Nao tem colisao (da para atravessar). Mao vazia nele: recolhe o gancho (e a corda some junto).
public class HookBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<HookBlock> CODEC = simpleCodec(HookBlock::new);

    private static final VoxelShape FLOOR = Block.box(4, 0, 4, 12, 13, 12);
    private static final VoxelShape CEILING = Block.box(4, 3, 4, 12, 16, 12);
    private static final VoxelShape WALL_N = Block.box(4, 4, 3, 12, 12, 16);    // gancho olhando para o norte (preso na parede sul)
    private static final VoxelShape WALL_S = Block.box(4, 4, 0, 12, 12, 13);
    private static final VoxelShape WALL_E = Block.box(0, 4, 4, 13, 12, 12);
    private static final VoxelShape WALL_W = Block.box(3, 4, 4, 16, 12, 12);

    public HookBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> WALL_N;
                case SOUTH -> WALL_S;
                case EAST -> WALL_E;
                default -> WALL_W;
            };
        };
    }

    // Estado do gancho para a face do bloco que foi atingida (UP = chao, DOWN = teto, lados = parede)
    public static BlockState stateFor(Direction hitFace, Direction playerDir) {
        BlockState base = ModBlocks.CLIMBING_HOOK.get().defaultBlockState();
        if (hitFace == Direction.UP) return base.setValue(FACE, AttachFace.FLOOR).setValue(FACING, playerDir);
        if (hitFace == Direction.DOWN) return base.setValue(FACE, AttachFace.CEILING).setValue(FACING, playerDir);
        return base.setValue(FACE, AttachFace.WALL).setValue(FACING, hitFace);
    }

    // Posicoes candidatas para o inicio da corda: no chao, ao redor (preferindo a direcao do gancho); na parede/teto, logo abaixo.
    private static BlockPos[] candidates(BlockState state, BlockPos hookPos) {
        if (state.getValue(FACE) != AttachFace.FLOOR) return new BlockPos[]{hookPos.below()};
        Direction f = state.getValue(FACING);
        BlockPos[] r = new BlockPos[4];
        int i = 0;
        r[i++] = hookPos.relative(f);
        for (Direction d : Direction.Plane.HORIZONTAL) if (d != f) r[i++] = hookPos.relative(d);
        return r;
    }

    // Onde a corda pode pender (precisa de espaco livre ali e logo abaixo). Null se nao houver lugar.
    @Nullable
    public static BlockPos findRopeStart(Level level, BlockPos hookPos, BlockState state) {
        for (BlockPos c : candidates(state, hookPos)) {
            if (RopeBlock.isFree(level.getBlockState(c)) && RopeBlock.isFree(level.getBlockState(c.below()))) return c;
        }
        return null;
    }

    // Onde esta a corda ja amarrada neste gancho (null se nao houver)
    @Nullable
    private static BlockPos findAnchoredRope(Level level, BlockPos hookPos, BlockState state) {
        for (BlockPos c : candidates(state, hookPos)) {
            BlockState s = level.getBlockState(c);
            if (s.is(ModBlocks.ROPE.get()) && s.getValue(RopeBlock.ANCHORED)) return c;
        }
        return null;
    }

    // Se o gancho some (quebrado ou recolhido), a corda dele some junto
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos rope = findAnchoredRope(level, pos, state);
            if (rope != null) RopeBlock.pullChain(level, rope, null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Com algo na mao o clique vai para o item; com a mao vazia cai em useWithoutItem (recolher)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);   // onRemove tira a corda
            if (!player.hasInfiniteMaterials()) {
                ItemStack give = new ItemStack(ModItems.CLIMBING_HOOK.get());
                player.getInventory().add(give);
                if (!give.isEmpty()) player.drop(give, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
