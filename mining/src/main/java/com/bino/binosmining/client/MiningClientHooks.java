package com.bino.binosmining.client;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;
import java.util.function.Predicate;

// Ganchos do lado do cliente para outros mods do conjunto (ver MiningHooks).
public final class MiningClientHooks {
    private MiningClientHooks() {}

    // botao "Receitas+" na cena da bancada: abre o navegador de receitas do Vanilla+ (null = sem botao)
    public static Function<Screen, Screen> recipeBrowser = null;
    public static Predicate<Screen> isRecipeBrowser = s -> false;
}
