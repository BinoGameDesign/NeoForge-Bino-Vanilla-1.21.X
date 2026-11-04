package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Foto revelada: so decoracao. Na moldura mostra a imagem inteira (varias lado a lado formam um quadro).
public class PhotoItem extends Item {
    public PhotoItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModDataComponents.PhotoData d = stack.get(ModDataComponents.PHOTO.get());
        if (d != null) tooltip.add(Component.translatable("tooltip.binosvanillaplus.photo", d.day()).withStyle(ChatFormatting.GRAY));
    }
}
