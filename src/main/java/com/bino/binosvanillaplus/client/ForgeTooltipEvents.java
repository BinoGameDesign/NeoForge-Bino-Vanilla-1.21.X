package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.util.ForgeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Mostra "Forja +n" na dica dos equipamentos forjados
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class ForgeTooltipEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        int level = ForgeUtil.getLevel(event.getItemStack());
        if (level > 0) {
            event.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.forged",
                    level, level * 10).withStyle(ChatFormatting.GOLD));
        }
    }
}
