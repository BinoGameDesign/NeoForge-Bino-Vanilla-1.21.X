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

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean fahrenheit() {
        try { return FAHRENHEIT.get(); } catch (IllegalStateException e) { return false; }
    }
}
