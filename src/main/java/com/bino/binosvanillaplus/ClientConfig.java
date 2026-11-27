package com.bino.binosvanillaplus;

import net.neoforged.neoforge.common.ModConfigSpec;

// Opcoes do cliente (cada jogador escolhe a sua): aparecem no botao "Config" do mod.
public final class ClientConfig {
    private ClientConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FAHRENHEIT = BUILDER
            .comment("Temperatura em Fahrenheit (°F) em vez de Celsius (°C)")
            .translation("binosvanillaplus.configuration.fahrenheit")
            .define("fahrenheit", false);

    public static final ModConfigSpec.IntValue HUD_SCALE = BUILDER
            .comment("Tamanho do visor de baixo (hotbar, vida, fome, sede, experiencia, temperatura, estamina), em %, por cima da Escala da interface")
            .translation("binosvanillaplus.configuration.hudScale")
            .defineInRange("hudScale", 115, 75, 150);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static float hudScale() {
        try { return HUD_SCALE.get() / 100f; } catch (IllegalStateException e) { return 1f; }
    }

    public static boolean fahrenheit() {
        try { return FAHRENHEIT.get(); } catch (IllegalStateException e) { return false; }
    }
}
