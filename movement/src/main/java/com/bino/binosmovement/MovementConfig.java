package com.bino.binosmovement;

import net.neoforged.neoforge.common.ModConfigSpec;

// Opcoes do Movement (arquivo config/binosmovement-client.toml e a tela de opcoes do mod)
public final class MovementConfig {
    private MovementConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // camera de primeira pessoa presa a cabeca da animacao (cambalhota gira a tela, estocada leva a camera junto...)
    public static final ModConfigSpec.BooleanValue HEAD_CAMERA = B.define("headCamera", true);
    // quanto do movimento do corpo vai para a camera (0 = nada, 100 = tudo)
    public static final ModConfigSpec.IntValue CAMERA_INTENSITY = B.defineInRange("cameraIntensity", 100, 0, 100);
    // conforto: cambalhota/rodopio/tombo nao viram a tela inteira (no maximo SHORT_SPIN_MAX graus)
    public static final ModConfigSpec.BooleanValue SHORT_SPINS = B.define("shortSpins", false);
    // ver o proprio corpo (tronco e pernas) em primeira pessoa
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_BODY = B.define("firstPersonBody", true);

    public static final ModConfigSpec SPEC = B.build();

    public static final float SHORT_SPIN_MAX = 35f;

    // leitura segura (antes de o arquivo carregar, vale o padrao)
    public static boolean headCamera() { return SPEC.isLoaded() ? HEAD_CAMERA.get() : true; }
    public static float intensity() { return SPEC.isLoaded() ? CAMERA_INTENSITY.get() / 100f : 1f; }
    public static boolean shortSpins() { return SPEC.isLoaded() && SHORT_SPINS.get(); }
    public static boolean firstPersonBody() { return SPEC.isLoaded() ? FIRST_PERSON_BODY.get() : true; }
}
