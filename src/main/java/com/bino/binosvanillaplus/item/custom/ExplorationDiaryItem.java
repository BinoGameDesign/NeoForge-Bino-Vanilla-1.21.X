package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.network.DiaryRequestPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

// Diario de Exploracao: abre as descobertas (duas por pagina) e, no fim, as fotos livres (revelar = 1 papel)
public class ExplorationDiaryItem extends Item {
    public ExplorationDiaryItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            player.playSound(SoundEvents.BOOK_PAGE_TURN, 1f, 1f);
            PacketDistributor.sendToServer(new DiaryRequestPayload(-1));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.exploration_diary").withStyle(ChatFormatting.GRAY));
    }
}
