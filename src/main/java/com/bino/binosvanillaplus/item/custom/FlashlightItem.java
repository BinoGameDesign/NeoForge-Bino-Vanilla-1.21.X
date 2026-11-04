package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

// Lanterna de luz focalizada: ilumina so para onde o jogador olha (ver gear/FlashlightManager).
// Botao direito liga e desliga. Tambem pode ir no capacete (capacete + lanterna na bancada).
public class FlashlightItem extends Item {
    public FlashlightItem(Properties properties) { super(properties); }

    public static boolean isOn(ItemStack stack) { return stack.getOrDefault(ModDataComponents.FLASHLIGHT_ON.get(), false); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !isOn(stack);
        stack.set(ModDataComponents.FLASHLIGHT_ON.get(), on);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                on ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.PLAYERS, 0.6f, on ? 1.6f : 1.3f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isOn(stack) ? "tooltip.binosvanillaplus.flashlight.on" : "tooltip.binosvanillaplus.flashlight.off")
                .withStyle(isOn(stack) ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.flashlight.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();   // ligar/desligar nao "troca" o item na mao
    }
}
