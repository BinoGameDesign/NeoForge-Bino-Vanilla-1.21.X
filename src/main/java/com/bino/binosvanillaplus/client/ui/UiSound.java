package com.bino.binosvanillaplus.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

// Sons de interface para cliques em elementos que nao sao botoes do vanilla (linhas, cores, itens de menu...)
public final class UiSound {
    private UiSound() {}

    public static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f, 0.6f));
    }

    // clique mais agudo e baixo, para selecoes (cor, icone, item de lista)
    public static void tick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6f, 0.35f));
    }
}
