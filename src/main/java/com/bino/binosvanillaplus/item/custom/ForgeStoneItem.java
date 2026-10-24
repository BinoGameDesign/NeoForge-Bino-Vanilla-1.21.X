package com.bino.binosvanillaplus.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Pedra de forja de um nível (1 a 10). Ela não faz nada sozinha: é usada na bigorna (veja ForgeEvents).
public class ForgeStoneItem extends Item {
    private final int level;

    public ForgeStoneItem(int level, Properties properties) {
        super(properties);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.binosvanillaplus.forge_stone_bonus", level * 10)
                .withStyle(ChatFormatting.GOLD));
        // Nível 1 vai em equipamento sem forja; os outros precisam do nível anterior
        tooltip.add(level == 1
                ? Component.translatable("tooltip.binosvanillaplus.forge_stone_first").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.binosvanillaplus.forge_stone_use", level - 1).withStyle(ChatFormatting.GRAY));
    }
}
