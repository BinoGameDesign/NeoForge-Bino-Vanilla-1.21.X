package com.bino.binosvanillaplus.client.ui;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

// Dicas (tooltips) de itens: linhas longas quebram em ~240 px em vez de atravessar a tela inteira.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class TooltipWrap {
    private TooltipWrap() {}

    @SubscribeEvent
    public static void onGather(RenderTooltipEvent.GatherComponents e) {
        if (e.getMaxWidth() <= 0 || e.getMaxWidth() > 240) e.setMaxWidth(240);
    }
}
