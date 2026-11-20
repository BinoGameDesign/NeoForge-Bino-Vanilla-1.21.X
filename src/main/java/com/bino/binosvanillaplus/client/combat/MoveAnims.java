package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.combat.CombatMove;

// Animacoes dos golpes (mod Combat), uma por golpe e por arma (espada e machado, sempre uma mao so).
//
// Cada chave descreve a pose da MAO DA ARMA vista da camera (primeira pessoa) e do corpo (terceira pessoa):
//   hx, hy, hz = onde a mao esta (blocos; x direita, y cima, z negativo = para a frente)
//   ay = para onde a ponta aponta, de lado (graus; + direita)    ap = de cima (0 = ponta para cima, 90 = para a frente, 180 = para baixo)
//   er = giro da lamina no proprio eixo (graus; 90 = fio de lado, para cortes horizontais)
//   tw = giro do tronco (+ direita)   ln = inclinacao para a frente   sp = giro do corpo inteiro (rodopio)   hop = pulinho (blocos)
//   st = passo (perna da frente)
// Em terceira pessoa o braco aponta do ombro para a mao e a lamina gira para bater com a mesma direcao.
// Cada chave tem a curva do trecho que TERMINA nela: W = preparacao (suave), S = golpe (acelera ate o impacto),
// F = continuacao (desacelera), R = volta ao descanso.
public final class MoveAnims {
    private MoveAnims() {}

    static final int HX = 0, HY = 1, HZ = 2, AY = 3, AP = 4, ER = 5, TW = 6, LN = 7, SP = 8, HOP = 9, ST = 10, N = 11;
    static final int LIN = 0, W = 1, S = 2, F = 3, R = 4;

    // descanso (igual a mao parada com a arma)
    static final float[] REST = {0.50f, -0.50f, -0.72f, -8, 35, 0, 0, 0, 0, 0, 0};

    record Key(float t, int ease, float[] v) {}

    private static Key k(float t, int ease, float hx, float hy, float hz, float ay, float ap, float er,
                         float tw, float ln, float sp, float hop, float st) {
        return new Key(t, ease, new float[]{hx, hy, hz, ay, ap, er, tw, ln, sp, hop, st});
    }
    private static Key rest(float t, float spin) {
        float[] v = REST.clone(); v[SP] = spin; return new Key(t, R, v);
    }
    private static Key rest0() { return new Key(0, LIN, REST.clone()); }

    static Key[] keys(CombatMove m, boolean axe) { return axe ? axe(m) : sword(m); }

    // ------------------------------------------------------------------ ESPADA: rapida, cortes limpos, estocada
    private static Key[] sword(CombatMove m) {
        return switch (m) {
            // corte diagonal de cima-direita para baixo-esquerda
            case LIGHT_1 -> new Key[]{rest0(),
                    k(0.22f, W, 0.66f, 0.06f, -0.42f, 55, 15, -35, 28, -4, 0, 0, 0),
                    k(0.34f, S, 0.02f, -0.30f, -0.82f, -30, 110, -60, -8, 10, 0, 0, 0.35f),
                    k(0.50f, F, -0.44f, -0.56f, -0.62f, -78, 142, -60, -30, 12, 0, 0, 0.35f),
                    rest(1, 0)};
            // reves horizontal da esquerda para a direita
            case LIGHT_2 -> new Key[]{rest0(),
                    k(0.24f, W, -0.42f, -0.24f, -0.48f, -100, 84, 90, -34, 0, 0, 0, 0),
                    k(0.34f, S, 0.04f, -0.28f, -0.86f, -4, 88, 90, 0, 6, 0, 0, 0.3f),
                    k(0.52f, F, 0.58f, -0.32f, -0.52f, 85, 92, 90, 34, 8, 0, 0, 0.3f),
                    rest(1, 0)};
            // corte vertical de cima
            case LIGHT_3 -> new Key[]{rest0(),
                    k(0.24f, W, 0.30f, 0.26f, -0.34f, 2, -28, 0, 10, -10, 0, 0, 0),
                    k(0.34f, S, 0.08f, -0.26f, -0.86f, -4, 112, 0, 0, 16, 0, 0, 0.45f),
                    k(0.52f, F, 0.05f, -0.58f, -0.70f, -4, 152, 0, 0, 20, 0, 0, 0.45f),
                    rest(1, 0)};
            // pesado: arma vai para tras do ombro, segura, e desce em diagonal com o corpo todo
            case HEAVY -> new Key[]{rest0(),
                    k(0.36f, W, 0.72f, 0.22f, -0.18f, 75, -12, -20, 45, -8, 0, 0, 0.25f),
                    k(0.44f, LIN, 0.74f, 0.26f, -0.15f, 78, -16, -20, 48, -10, 0, 0, 0.25f),
                    k(0.50f, S, -0.10f, -0.36f, -0.90f, -35, 122, -50, -20, 22, 0, 0, 0.7f),
                    k(0.66f, F, -0.52f, -0.62f, -0.55f, -82, 152, -50, -40, 26, 0, 0, 0.7f),
                    rest(1, 0)};
            // no ar: ergue por cima da cabeca e crava para baixo
            case JUMP -> new Key[]{rest0(),
                    k(0.30f, W, 0.24f, 0.36f, -0.24f, 0, -42, 0, 0, -16, 0, 0.25f, 0),
                    k(0.55f, S, 0.05f, -0.40f, -0.86f, -2, 136, 0, 0, 30, 0, 0, 0.3f),
                    k(0.72f, F, 0.05f, -0.56f, -0.74f, -2, 160, 0, 0, 30, 0, 0, 0.3f),
                    rest(1, 0)};
            // investida: estocada reta
            case FORWARD -> new Key[]{rest0(),
                    k(0.20f, W, 0.42f, -0.36f, -0.24f, -4, 88, 90, 22, -6, 0, 0, 0),
                    k(0.32f, S, 0.12f, -0.25f, -1.16f, -3, 90, 90, -16, 22, 0, 0, 0.9f),
                    k(0.52f, F, 0.12f, -0.26f, -1.05f, -3, 90, 90, -16, 22, 0, 0, 0.9f),
                    rest(1, 0)};
            // final: corte largo girando o corpo (rodopio de 360) e golpe de cima para fechar
            case FINISHER -> new Key[]{rest0(),
                    k(0.18f, W, 0.66f, -0.10f, -0.30f, 92, 88, -90, 42, 0, 0, 0, 0.2f),
                    k(0.34f, S, -0.56f, -0.26f, -0.55f, -92, 88, -90, -42, 6, 180, 0, 0.3f),
                    k(0.46f, F, 0.70f, 0.30f, -0.24f, 62, -12, -20, 30, -8, 300, 0.1f, 0.3f),
                    k(0.62f, S, -0.05f, -0.38f, -0.90f, -30, 126, -40, -12, 26, 360, 0, 0.8f),
                    k(0.80f, F, -0.42f, -0.60f, -0.60f, -72, 156, -40, -30, 26, 360, 0, 0.8f),
                    rest(1, 360)};
        };
    }

    // ------------------------------------------------------------------ MACHADO: pesado, arcos largos, machadadas
    private static Key[] axe(CombatMove m) {
        return switch (m) {
            // machadada de cima, por cima do ombro
            case LIGHT_1 -> new Key[]{rest0(),
                    k(0.26f, W, 0.56f, 0.30f, -0.28f, 36, -24, -10, 32, -8, 0, 0, 0),
                    k(0.36f, S, 0.02f, -0.32f, -0.86f, -20, 126, -15, -6, 18, 0, 0, 0.4f),
                    k(0.56f, F, -0.30f, -0.66f, -0.55f, -42, 166, -15, -22, 22, 0, 0, 0.4f),
                    rest(1, 0)};
            // gancho horizontal baixo da esquerda para a direita (fio na frente)
            case LIGHT_2 -> new Key[]{rest0(),
                    k(0.26f, W, -0.46f, -0.22f, -0.40f, -104, 80, 90, -42, 4, 0, 0, 0),
                    k(0.36f, S, 0.05f, -0.32f, -0.86f, 0, 86, 90, 0, 10, 0, 0, 0.35f),
                    k(0.56f, F, 0.62f, -0.30f, -0.44f, 98, 90, 90, 42, 8, 0, 0, 0.35f),
                    rest(1, 0)};
            // de baixo para cima (uppercut)
            case LIGHT_3 -> new Key[]{rest0(),
                    k(0.26f, W, 0.46f, -0.66f, -0.40f, 30, 166, 180, 26, 14, 0, 0, 0.2f),
                    k(0.36f, S, 0.00f, -0.06f, -0.86f, -10, 60, 180, -10, -4, 0, 0, 0.2f),
                    k(0.56f, F, -0.30f, 0.26f, -0.54f, -40, 10, 180, -26, -12, 0, 0, 0.2f),
                    rest(1, 0)};
            // pesado: ergue atras da cabeca e racha o chao
            case HEAVY -> new Key[]{rest0(),
                    k(0.38f, W, 0.34f, 0.42f, -0.08f, 12, -62, 0, 16, -16, 0, 0, 0.2f),
                    k(0.45f, LIN, 0.34f, 0.46f, -0.05f, 12, -68, 0, 16, -18, 0, 0, 0.2f),
                    k(0.50f, S, 0.05f, -0.46f, -0.86f, -5, 142, 0, 0, 32, 0, 0, 0.75f),
                    k(0.72f, F, 0.05f, -0.62f, -0.74f, -5, 166, 0, 0, 34, 0, 0, 0.75f),
                    rest(1, 0)};
            // no ar: cambalhota para a frente e machadada
            case JUMP -> new Key[]{rest0(),
                    k(0.30f, W, 0.20f, 0.46f, -0.14f, 0, -72, 0, 0, -22, 0, 0.3f, 0),
                    k(0.55f, S, 0.05f, -0.50f, -0.86f, -4, 150, 0, 0, 38, 0, 0, 0.35f),
                    k(0.76f, F, 0.05f, -0.62f, -0.74f, -4, 168, 0, 0, 38, 0, 0, 0.35f),
                    rest(1, 0)};
            // investida: ombro na frente e golpe horizontal da direita para a esquerda
            case FORWARD -> new Key[]{rest0(),
                    k(0.18f, W, 0.64f, -0.20f, -0.30f, 88, 85, -90, 46, 10, 0, 0, 0.3f),
                    k(0.32f, S, -0.05f, -0.30f, -0.96f, -10, 88, -90, -10, 22, 0, 0, 0.9f),
                    k(0.52f, F, -0.56f, -0.36f, -0.54f, -92, 90, -90, -46, 18, 0, 0, 0.9f),
                    rest(1, 0)};
            // final: redemoinho (dois rodopios com o machado na horizontal) e machadada de cima
            case FINISHER -> new Key[]{rest0(),
                    k(0.12f, W, 0.66f, -0.26f, -0.30f, 92, 85, -90, 46, 4, 0, 0, 0.3f),
                    k(0.28f, S, -0.56f, -0.30f, -0.50f, -92, 85, -90, -46, 6, 360, 0, 0.3f),
                    k(0.36f, LIN, 0.66f, -0.26f, -0.30f, 92, 85, -90, 46, 4, 450, 0, 0.3f),
                    k(0.48f, S, -0.56f, -0.30f, -0.50f, -92, 85, -90, -46, 6, 720, 0, 0.3f),
                    k(0.56f, W, 0.32f, 0.42f, -0.10f, 12, -62, 0, 14, -16, 720, 0.15f, 0.3f),
                    k(0.62f, S, 0.05f, -0.46f, -0.86f, -5, 142, 0, 0, 32, 720, 0, 0.8f),
                    k(0.84f, F, 0.05f, -0.62f, -0.74f, -5, 166, 0, 0, 34, 720, 0, 0.8f),
                    rest(1, 720)};
        };
    }

    private static float ease(int e, float a) {
        return switch (e) {
            case W, R -> a * a * (3 - 2 * a);
            case S -> a * a * a;                                   // acelera ate o impacto
            case F -> 1 - (1 - a) * (1 - a) * (1 - a);              // sai rapido e freia
            default -> a;
        };
    }

    // pose no tempo u (0..1)
    static float[] sample(Key[] k, float u) {
        float[] out = new float[N];
        if (u <= k[0].t()) { System.arraycopy(k[0].v(), 0, out, 0, N); return out; }
        for (int i = 0; i < k.length - 1; i++) {
            if (u <= k[i + 1].t()) {
                float a = (u - k[i].t()) / Math.max(1e-4f, k[i + 1].t() - k[i].t());
                a = ease(k[i + 1].ease(), a);
                for (int j = 0; j < N; j++) out[j] = k[i].v()[j] + (k[i + 1].v()[j] - k[i].v()[j]) * a;
                return out;
            }
        }
        System.arraycopy(k[k.length - 1].v(), 0, out, 0, N);
        return out;
    }

    // o trecho de golpe (para o rastro da lamina): do fim da preparacao ate o fim da continuacao
    static boolean striking(Key[] k, float u) {
        for (int i = 1; i < k.length; i++) {
            if (k[i].ease() == S && u >= k[i - 1].t() - 0.02f && u <= k[i].t()) return true;
            if (k[i].ease() == F && u >= k[i - 1].t() && u <= k[i - 1].t() + (k[i].t() - k[i - 1].t()) * 0.6f) return true;
        }
        return false;
    }
}
