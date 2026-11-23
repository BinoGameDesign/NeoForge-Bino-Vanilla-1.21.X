package com.bino.binosvanillaplus.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Essencia do Recomeco (rara: baus das cavernas): usar devolve todos os pontos de atributo (o nivel continua o mesmo)
public class RespecEssenceItem extends Item {
    public RespecEssenceItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            com.bino.binosvanillaplus.leveling.PlayerStats.respec(sp);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
            if (!player.isCreative()) st.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
}
