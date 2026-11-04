package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Camera fotografica: aponte e clique. Minerios e fosseis viram descobertas no Diario de Exploracao;
// o resto vira foto guardada no fim do diario. Um flash pequeno e o som do obturador. Sem interface na tela.
public class CameraItem extends Item {
    public CameraItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.bino.binosvanillaplus.client.journal.PhotoCapture.shoot();
        } else if (level instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 1f, 1f);
            Vec3 f = player.getEyePosition().add(player.getLookAngle().scale(0.6));
            sl.sendParticles(ParticleTypes.FLASH, f.x, f.y, f.z, 1, 0, 0, 0, 0);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.camera").withStyle(ChatFormatting.GRAY));
    }
}
