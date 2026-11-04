package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// Sonar de exploracao: um pulso que revela por alguns segundos, com contorno branco, minerios, fosseis e criaturas
// perto (ver client/gear/SonarClient). Recarga de 12 s.
public class ExplorerSonarItem extends Item {
    public static final int COOLDOWN = 20 * 12;

    public ExplorerSonarItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SONAR_PING.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        if (level.isClientSide) com.bino.binosvanillaplus.client.gear.SonarClient.pulse(player);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.explorer_sonar").withStyle(ChatFormatting.GRAY));
    }
}
