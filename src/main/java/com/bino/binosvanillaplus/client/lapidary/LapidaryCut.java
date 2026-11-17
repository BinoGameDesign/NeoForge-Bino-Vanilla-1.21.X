package com.bino.binosvanillaplus.client.lapidary;

// Linha do tempo de uma lapidacao (em segundos), usada pela cena em primeira pessoa e pelos outros jogadores.
// Ritmo baseado nas estacoes do Cinematic Interactions (corte do cortador de pedra: ~1 s; "tudo": +0,5 s), esticado
// para as etapas daqui:
//   0,00-0,36  mao esquerda aperta o botao
//   0,30-1,05  lapidador desce devagar ate encostar no minerio (contato em ~0,95)
//   0,85-1,20  disco acelera; gira; 1,80-2,30 desacelera ate parar (faiscas enquanto gira)
//   1,55       o minerio vira gema
//   2,30-2,75  lapidador sobe
//   2,50-2,80  mao direita vai ate o disco e pega a gema
//   2,80-3,30  leva a gema ate o canto das gemas e solta (3,30 = o menu lapida de verdade)
//   3,30-3,55  mao volta
// "Lapidar tudo" (Shift) gira 0,8 s a mais.
public final class LapidaryCut {
    private LapidaryCut() {}

    public static final double MAX_REV_PER_S = 2.4;

    public static double extra(boolean all) { return all ? 0.8 : 0.0; }
    public static double length(boolean all) { return 3.55 + extra(all); }
    public static final double CONTACT = 0.95;   // o lapidador encosta na pedra
    public static double reachAt(boolean all) { return 2.50 + extra(all); }   // a mao direita chega ao disco
    public static double gemAt(boolean all) { return 1.55 + extra(all) * 0.5; }
    public static double pickAt(boolean all) { return 2.80 + extra(all); }
    public static double placeAt(boolean all) { return 3.30 + extra(all); }

    // 0 a 1
    public static double speed(double t, boolean all) {
        double e = extra(all);
        return smoother(window(t, 0.85, 1.20)) * (1 - smoother(window(t, 1.80 + e, 2.30 + e)));
    }

    // pixels que o lapidador desceu
    public static double armDrop(double t, boolean all) {
        double e = extra(all);
        return LapidaryGeo.ARM_DROP * (smoother(window(t, 0.30, 1.05)) - smoother(window(t, 2.30 + e, 2.75 + e)));
    }

    // pixels que o botao afundou
    public static double button(double t) {
        return LapidaryGeo.BUTTON_PRESS * hump(window(t, 0.18, 0.40));
    }

    // angulo do disco (graus): integral da velocidade (tabela, para ser igual em todo cliente)
    private static final double STEP = 1.0 / 240.0;
    private static final double[][] ANGLE = {table(false), table(true)};

    private static double[] table(boolean all) {
        int n = (int) Math.ceil(length(all) / STEP) + 2;
        double[] a = new double[n];
        for (int i = 1; i < n; i++) a[i] = a[i - 1] + speed((i - 0.5) * STEP, all) * MAX_REV_PER_S * 360.0 * STEP;
        return a;
    }

    public static double angle(double t, boolean all) {
        double[] a = ANGLE[all ? 1 : 0];
        if (t <= 0) return 0;
        double f = t / STEP;
        int i = (int) f;
        if (i >= a.length - 1) return a[a.length - 1];
        return a[i] + (a[i + 1] - a[i]) * (f - i);
    }

    // escala do "pulo" da gema quando o minerio se transforma
    public static double pop(double t, boolean all) {
        double u = window(t, gemAt(all), gemAt(all) + 0.35);
        return u <= 0 || u >= 1 ? 1.0 : 1.0 + 0.35 * hump(u) ;
    }

    // ---- curvas
    public static double clamp01(double t) { return t < 0 ? 0 : t > 1 ? 1 : t; }
    public static double window(double t, double a, double b) { return clamp01((t - a) / (b - a)); }
    public static double smoother(double t) { t = clamp01(t); return t * t * t * (t * (t * 6 - 15) + 10); }
    public static double inOutCubic(double t) { t = clamp01(t); return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    public static double hump(double t) { return Math.sin(Math.PI * clamp01(t)); }
}
