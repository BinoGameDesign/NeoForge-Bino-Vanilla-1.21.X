package com.bino.binosvanillaplus.thirst;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

// Bebida de temperatura (garrafa): Cha Quente aquece, Refresco refresca. Mata a sede e devolve a garrafa.
public class TemperatureDrinkItem extends Item {
    private final boolean cool;
    private final int seconds;
    private final float thirst;

    public TemperatureDrinkItem(boolean cool, int seconds, float thirst, Properties props) {
        super(props);
        this.cool = cool; this.seconds = seconds; this.thirst = thirst;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
            Thirst.add(sp, thirst);
            Temperature.refresh(sp, cool, seconds, 25f);
        }
        if (entity instanceof Player p && p.hasInfiniteMaterials()) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public SoundEvent getEatingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { return ItemUtils.startUsingInstantly(level, player, hand); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(cool ? "tooltip.binosvanillaplus.drink.cool" : "tooltip.binosvanillaplus.drink.warm", seconds)
                .withStyle(cool ? ChatFormatting.AQUA : ChatFormatting.GOLD));
    }
}
