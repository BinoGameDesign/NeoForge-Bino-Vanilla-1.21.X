package com.bino.binoscombat.combat;

// Brilho do parry (mod Combat): a janela antes do golpe (ou da flecha chegar) e' dividida em faixas de cor:
//   30% BRANCO | 15% AMARELO | 10% AZUL CLARO | 15% AMARELO | 30% BRANCO      (termina no momento do golpe)
//   branco  = so bloqueia (golpes e flechas)
//   amarelo = parry de golpe fisico: o mob fica atordoado um instante e a postura enche bem (flecha: so bloqueia)
//   azul    = Issen na hora (golpe enorme); flechas e projeteis voltam para quem atirou
public final class ParryZones {
    private ParryZones() {}

    public static final int WINDOW_MS = 460;   // tamanho da janela (menor se o golpe do mob for mais rapido)

    // u = 0 no comeco da janela, 1 no golpe
    public static byte zone(float u) {
        if (u < 0.30f || u > 0.70f) return 0;
        if (u < 0.45f || u > 0.55f) return 1;
        return 2;
    }

    // janela para um golpe que demora windupMs para sair
    public static long window(long windupMs) { return Math.min(WINDOW_MS, (long) (windupMs * 0.85f)); }
}
