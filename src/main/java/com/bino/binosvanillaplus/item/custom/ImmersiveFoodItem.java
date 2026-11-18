package com.bino.binosvanillaplus.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Comida com animacao propria de comer (ver client/eat/EatAnimation): o tempo de uso = duracao da animacao.
public class ImmersiveFoodItem extends Item {
    private final int useTicks;
    public ImmersiveFoodItem(Properties props, int useTicks) { super(props); this.useTicks = useTicks; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return useTicks; }
}
