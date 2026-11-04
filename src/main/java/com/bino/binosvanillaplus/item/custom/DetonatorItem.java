package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.block.custom.DynamiteBlock;
import com.bino.binosvanillaplus.gear.Dynamite;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// Detonador: botao direito explode, uma depois da outra, as dinamites que VOCE colocou (ate 64 blocos)
public class DetonatorItem extends Item {
    public static final double RANGE = 64.0;

    public DetonatorItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8f, 0.7f);
        if (level instanceof ServerLevel sl) {
            List<BlockPos> list = Dynamite.ownedNear(player.getUUID(), sl, player.blockPosition(), RANGE);
            if (list.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.binosvanillaplus.detonator.none").withStyle(ChatFormatting.GRAY), true);
            } else {
                for (int i = 0; i < list.size(); i++) DynamiteBlock.prime(sl, list.get(i), 2 + i * 4);   // em sequencia
                player.displayClientMessage(Component.translatable("message.binosvanillaplus.detonator.fire", list.size()).withStyle(ChatFormatting.GOLD), true);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.detonator").withStyle(ChatFormatting.GRAY));
    }
}
