package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Caldeirao com leite. 2 baldes de leite enchem; com fogueira ACESA embaixo o leite vai coalhando (COOK 0..5)
// (0-2 leite branco, 3-5 coalhando = creme amarelado) e vira queijo (COOK 6). Clique direito no queijo pronto: pega o queijo e o caldeirao esvazia.
public class MilkCauldronBlock extends Block {
    public static final MapCodec<MilkCauldronBlock> CODEC = simpleCodec(MilkCauldronBlock::new);

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final int STEP_TICKS = 200;   // 10 s por etapa; 6 etapas = 1 minuto para virar queijo

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 2);   // baldes de leite
    public static final IntegerProperty COOK = IntegerProperty.create("cook", 0, 6);     // 6 = queijo pronto
    public static final int READY = 6;

    public MilkCauldronBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(COOK, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEVEL, COOK); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Blocks.CAULDRON.defaultBlockState().getShape(l, p, c);
    }

    public static boolean onLitCampfire(Level level, BlockPos pos) {
        return CampfireBlock.isLitCampfire(level.getBlockState(pos.below()));
    }

    // ------------------------------------------------------------------ cozinhar
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && state.getValue(LEVEL) == 2 && state.getValue(COOK) < READY) level.scheduleTick(pos, this, STEP_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) != 2 || state.getValue(COOK) >= READY) return;
        if (onLitCampfire(level, pos)) {
            int c = state.getValue(COOK) + 1;
            level.setBlock(pos, state.setValue(COOK, c), 3);
            if (c == READY) level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        if (level.getBlockState(pos).getValue(COOK) < READY) level.scheduleTick(pos, this, STEP_TICKS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(LEVEL) == 2 && state.getValue(COOK) < READY && onLitCampfire(level, pos) && r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.25 + r.nextDouble() * 0.5, pos.getY() + 0.95, pos.getZ() + 0.25 + r.nextDouble() * 0.5, 0, 0.02, 0);
            if (r.nextInt(8) == 0) level.playLocalSound(pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4f, 0.8f, false);
        }
    }

    // ------------------------------------------------------------------ interacoes
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(COOK) >= READY) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // pegar o queijo
        if (stack.is(Items.MILK_BUCKET) && state.getValue(LEVEL) == 1) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.setBlock(pos, state.setValue(LEVEL, 2).setValue(COOK, 0), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BUCKET)) {   // tira o leite de volta (so antes de virar queijo)
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.MILK_BUCKET)));
                level.setBlock(pos, state.getValue(LEVEL) == 2 ? state.setValue(LEVEL, 1).setValue(COOK, 0) : Blocks.CAULDRON.defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Texto de status (usado pelo Jade e pelo clique com a mao vazia)
    public static List<Component> describe(BlockState state, Level level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        int cook = state.getValue(COOK);
        if (cook >= READY) { out.add(Component.translatable("tooltip.binosvanillaplus.milk_cauldron.ready").withStyle(ChatFormatting.GOLD)); return out; }
        if (state.getValue(LEVEL) < 2) { out.add(Component.translatable("tooltip.binosvanillaplus.milk_cauldron.need_milk").withStyle(ChatFormatting.GRAY)); return out; }
        String phase = cook >= 3 ? "curdling" : "milk";
        int pct = cook * 100 / READY;
        out.add(Component.translatable("tooltip.binosvanillaplus.milk_cauldron.phase." + phase, pct).withStyle(ChatFormatting.YELLOW));
        if (onLitCampfire(level, pos)) {
            int secs = (READY - cook) * STEP_TICKS / 20;
            out.add(Component.translatable("tooltip.binosvanillaplus.milk_cauldron.time", secs).withStyle(ChatFormatting.GRAY));
        } else {
            out.add(Component.translatable("tooltip.binosvanillaplus.milk_cauldron.need_fire").withStyle(ChatFormatting.RED));
        }
        return out;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(COOK) < READY) {
            if (level.isClientSide) {
                MutableComponent msg = Component.empty();
                List<Component> lines = describe(state, level, pos);
                for (int i = 0; i < lines.size(); i++) { if (i > 0) msg.append(" · "); msg.append(lines.get(i)); }
                player.displayClientMessage(msg, true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack cheese = new ItemStack(ModItems.CHEESE.get());
            if (!player.getInventory().add(cheese)) player.drop(cheese, false);
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 0.9f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // balde de leite no caldeirao VAZIO do jogo -> caldeirao com 1 balde de leite
    public static void registerVanillaInteraction() {
        CauldronInteraction.EMPTY.map().put(Items.MILK_BUCKET, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.setBlockAndUpdate(pos, ModBlocks.MILK_CAULDRON.get().defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}
