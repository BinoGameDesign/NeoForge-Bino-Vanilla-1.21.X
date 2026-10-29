package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.entity.RopeBlockEntity;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Um segmento de corda (1 bloco). Escalavel como cipo e sem colisao.
//  - ANCHORED = true: faz parte de um gancho (qualquer um escala; some junto com o gancho; nao dropa item).
//  - ANCHORED = false: corda solta (jogada); quem a jogou NAO consegue escala-la (so recolher); outros jogadores sim.
public class RopeBlock extends Block implements EntityBlock {
    public static final BooleanProperty ANCHORED = BooleanProperty.create("anchored");
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);   // so a area clicavel

    public RopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ANCHORED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ANCHORED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RopeBlockEntity(pos, state);
    }

    // O dono de uma corda solta nao pode escalar nela
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        if (!state.getValue(ANCHORED) && level.getBlockEntity(pos) instanceof RopeBlockEntity be) {
            UUID owner = be.getOwner();
            if (owner != null && owner.equals(entity.getUUID())) return false;
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(ANCHORED)) return InteractionResult.PASS;   // corda do gancho: so escalar
        if (!level.isClientSide) pull(level, pos, state, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(ANCHORED)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) pull(level, pos, state, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // Corda solta so pode ser recolhida por quem a jogou
    public static void pull(Level level, BlockPos pos, BlockState state, Player player) {
        if (!state.getValue(ANCHORED) && level.getBlockEntity(pos) instanceof RopeBlockEntity be) {
            UUID owner = be.getOwner();
            if (owner != null && !owner.equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.binosvanillaplus.rope_not_yours"), true);
                return;
            }
        }
        pullChain(level, pos, player);
    }

    public static boolean isFree(BlockState s) {
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    // Pendura a corda a partir de "start" para baixo, ate "max" segmentos ou ate bater em algo. Retorna quantos colocou.
    public static int hang(Level level, BlockPos start, int max, boolean anchored, @Nullable UUID owner) {
        int placed = 0;
        BlockPos.MutableBlockPos p = start.mutable();
        while (placed < max && p.getY() >= level.getMinBuildHeight()) {
            if (!isFree(level.getBlockState(p))) break;
            level.setBlock(p, ModBlocks.ROPE.get().defaultBlockState().setValue(ANCHORED, anchored), 3);
            if (!anchored && owner != null && level.getBlockEntity(p) instanceof RopeBlockEntity be) be.setOwner(owner);
            placed++;
            p.move(Direction.DOWN);
        }
        return placed;
    }

    // Remove a corda inteira (do topo ate o fim). Corda solta devolve os itens; corda de gancho some sem devolver nada.
    public static int pullChain(Level level, BlockPos pos, @Nullable Player player) {
        Block rope = ModBlocks.ROPE.get();
        BlockPos top = pos;
        while (level.getBlockState(top.above()).is(rope)) top = top.above();
        boolean anchored = level.getBlockState(top).is(rope) && level.getBlockState(top).getValue(ANCHORED);
        int n = 0;
        BlockPos p = top;
        while (level.getBlockState(p).is(rope)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            n++;
            p = p.below();
        }
        if (n > 0 && !anchored && !(player != null && player.hasInfiniteMaterials())) {
            int left = n;
            while (left > 0) {
                ItemStack give = new ItemStack(ModItems.ROPE.get(), Math.min(left, 64));
                left -= give.getCount();
                if (player != null) {
                    player.getInventory().add(give);
                    if (!give.isEmpty()) player.drop(give, false);
                } else {
                    Block.popResource(level, pos, give);
                }
            }
        }
        if (n > 0) level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        return n;
    }
}
