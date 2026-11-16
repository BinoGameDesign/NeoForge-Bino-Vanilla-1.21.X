package com.bino.binosvanillaplus.client.lapidary;

import net.minecraft.world.item.ItemStack;

import java.util.List;

// Como uma mesa deve ser desenhada neste quadro (o LapidaryTableRenderer so le isto).
//  flint: silex na ponta do lapidador; disc: o que esta girando no disco (minerio ou, no fim, a gema);
//  discScale: "pulo" da gema; angle: giro do disco (graus); armDrop/button: pixels; visor: aceso;
//  pile: gemas prontas deixadas na mesa (so na propria cena); speed: velocidade do disco (0 a 1).
public record LapidaryView(ItemStack flint, ItemStack disc, float discScale, float angle, float armDrop, float button,
                           boolean visor, List<ItemStack> pile, float speed) {
    public static final LapidaryView IDLE = new LapidaryView(ItemStack.EMPTY, ItemStack.EMPTY, 1f, 0f, 0f, 0f, false, List.of(), 0f);
}
