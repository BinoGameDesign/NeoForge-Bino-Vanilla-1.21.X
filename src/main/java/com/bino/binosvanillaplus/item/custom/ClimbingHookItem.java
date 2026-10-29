package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.block.custom.HookBlock;
import com.bino.binosvanillaplus.block.custom.RopeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Gancho de escalada: botao direito LANCA o gancho para onde voce olha (ate RANGE blocos). Ele fica preso no chao, numa
// parede ou no teto e deixa pendurada uma corda de ROPE_LENGTH blocos. Para recolher: mao vazia no gancho.
public class ClimbingHookItem extends Item {
    public static final double RANGE = 32.0;
    public static final int ROPE_LENGTH = 10;   // 10 metros = 10 blocos

    public ClimbingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && throwHook((ServerLevel) level, player)) {
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean throwHook(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return fail(player, "message.binosvanillaplus.hook_miss");

        Direction face = hit.getDirection();
        BlockPos pos = hit.getBlockPos().relative(face);                 // o gancho fica no ar colado na superficie
        if (!RopeBlock.isFree(level.getBlockState(pos))) return fail(player, "message.binosvanillaplus.hook_miss");

        BlockState state = HookBlock.stateFor(face, player.getDirection());
        if (!state.canSurvive(level, pos)) return fail(player, "message.binosvanillaplus.hook_miss");
        BlockPos start = HookBlock.findRopeStart(level, pos, state);
        if (start == null) return fail(player, "message.binosvanillaplus.rope_no_space");

        level.setBlock(pos, state, 3);
        if (RopeBlock.hang(level, start, ROPE_LENGTH, true, null) <= 0) {
            level.removeBlock(pos, false);
            return fail(player, "message.binosvanillaplus.rope_no_space");
        }
        level.playSound(null, pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.4f);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private static boolean fail(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return false;
    }
}
