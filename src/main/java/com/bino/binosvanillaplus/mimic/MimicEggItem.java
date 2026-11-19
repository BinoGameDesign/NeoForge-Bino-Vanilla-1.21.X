package com.bino.binosvanillaplus.mimic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

// Ovo de Mimico: nao invoca nada no chao. Guarde num bau (simples ou duplo) e feche: o bau vira um mimico (MimicChest).
public class MimicEggItem extends Item {
    public MimicEggItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide && ctx.getPlayer() != null)
            ctx.getPlayer().displayClientMessage(Component.translatable("message.binosvanillaplus.mimic_egg_hint"), true);
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.mimic_egg").withStyle(ChatFormatting.GRAY));
    }
}
