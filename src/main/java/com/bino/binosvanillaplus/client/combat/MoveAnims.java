package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.combat.CombatMove;

// Animacoes dos golpes (mod Combat), uma por golpe e por arma. Cada chave: {tempo (0..1), sobe, varre, gira,
// empurra, levanta}:
//   sobe   = graus que a arma sobe (+) ou desce (-)
//   varre  = graus para a direita (+) ou esquerda (-) (o corte horizontal)
//   gira   = graus de giro da lamina (inclinacao)
//   empurra= blocos para a frente (estocada)
//   levanta= blocos para cima
// A mesma curva move a mao em primeira pessoa e o braco/corpo em terceira pessoa (CombatMoves).
// ESPADA: golpes rapidos e horizontais, estocada na investida. MACHADO: mais lentos (x1,25), pesados e verticais
// (machadadas), duas maos nos golpes grandes.
public final class MoveAnims {
    private MoveAnims() {}

    static float[][] keys(CombatMove m, boolean axe) {
        return axe ? axe(m) : sword(m);
    }

    private static float[][] sword(CombatMove m) {
        return switch (m) {
            case LIGHT_1 -> new float[][]{{0, 5, 40, -15, 0, 0}, {0.34f, 0, -60, 20, 0.05f, 0}, {1, 0, 0, 0, 0, 0}};          // da direita p/ esquerda
            case LIGHT_2 -> new float[][]{{0, 10, -50, 20, 0, 0}, {0.34f, 0, 50, -20, 0.05f, 0}, {1, 0, 0, 0, 0, 0}};         // de volta (revés)
            case LIGHT_3 -> new float[][]{{0, 45, 35, -25, 0, 0}, {0.34f, -30, -25, 15, 0.05f, 0}, {1, 0, 0, 0, 0, 0}};       // diagonal de cima
            case HEAVY -> new float[][]{{0, 0, 0, 0, 0, 0}, {0.4f, 75, 15, -10, -0.1f, 0.1f}, {0.5f, -40, -5, 5, 0.15f, -0.05f},
                    {0.75f, -35, -5, 5, 0.1f, -0.05f}, {1, 0, 0, 0, 0, 0}};                                                // ergue e desce com tudo
            case JUMP -> new float[][]{{0, 60, 0, 0, 0, 0.1f}, {0.55f, -55, 0, 0, 0.12f, -0.1f}, {0.8f, -45, 0, 0, 0.08f, -0.08f}, {1, 0, 0, 0, 0, 0}};
            case FORWARD -> new float[][]{{0, 0, 10, 0, -0.15f, 0}, {0.32f, 5, -5, 0, 0.55f, 0}, {0.6f, 5, -5, 0, 0.45f, 0}, {1, 0, 0, 0, 0, 0}};   // estocada
            case FINISHER -> new float[][]{{0, 15, 70, -30, 0, 0}, {0.3f, 0, -90, 30, 0.05f, 0}, {0.45f, 70, -20, -10, 0, 0.1f},
                    {0.62f, -45, 0, 0, 0.15f, -0.05f}, {0.85f, -40, 0, 0, 0.1f, 0}, {1, 0, 0, 0, 0, 0}};                      // corte largo + golpe de cima
        };
    }

    private static float[][] axe(CombatMove m) {
        return switch (m) {
            case LIGHT_1 -> new float[][]{{0, 50, 40, -20, 0, 0.05f}, {0.34f, -30, -20, 10, 0.08f, 0}, {1, 0, 0, 0, 0, 0}};     // machadada diagonal
            case LIGHT_2 -> new float[][]{{0, 5, -60, 25, 0, 0}, {0.34f, 0, 60, -25, 0.08f, 0}, {1, 0, 0, 0, 0, 0}};           // varrida pesada
            case LIGHT_3 -> new float[][]{{0, 80, 5, 0, -0.05f, 0.1f}, {0.34f, -45, 0, 0, 0.12f, -0.05f}, {1, 0, 0, 0, 0, 0}};  // de cima, rachando
            case HEAVY -> new float[][]{{0, 0, 0, 0, 0, 0}, {0.45f, 100, 0, 0, -0.15f, 0.15f}, {0.5f, -50, 0, 0, 0.2f, -0.1f},
                    {0.8f, -45, 0, 0, 0.15f, -0.1f}, {1, 0, 0, 0, 0, 0}};                                                  // duas maos, por cima da cabeca
            case JUMP -> new float[][]{{0, 90, 0, 0, 0, 0.15f}, {0.55f, -70, 0, 0, 0.18f, -0.12f}, {0.85f, -55, 0, 0, 0.12f, -0.1f}, {1, 0, 0, 0, 0, 0}};
            case FORWARD -> new float[][]{{0, 10, 60, -20, -0.1f, 0}, {0.32f, 0, -50, 20, 0.45f, 0}, {0.6f, 0, -50, 20, 0.35f, 0}, {1, 0, 0, 0, 0, 0}};  // investida girando
            case FINISHER -> new float[][]{{0, 10, 90, -30, 0, 0}, {0.3f, 5, -120, 35, 0.05f, 0}, {0.45f, 95, -10, 0, 0, 0.12f},
                    {0.62f, -60, 0, 0, 0.2f, -0.1f}, {0.9f, -50, 0, 0, 0.12f, -0.08f}, {1, 0, 0, 0, 0, 0}};                   // giro + machadada final
        };
    }

    // machado: duas maos nos golpes grandes
    static boolean twoHanded(CombatMove m, boolean axe) {
        return axe && (m == CombatMove.HEAVY || m == CombatMove.JUMP || m == CombatMove.FINISHER || m == CombatMove.LIGHT_3);
    }

    // valores no tempo u (0..1), suavizado entre as chaves
    static float[] sample(float[][] k, float u) {
        float[] out = new float[5];
        if (u <= k[0][0]) { System.arraycopy(k[0], 1, out, 0, 5); return out; }
        for (int i = 0; i < k.length - 1; i++) {
            if (u <= k[i + 1][0]) {
                float a = (u - k[i][0]) / Math.max(1e-4f, k[i + 1][0] - k[i][0]);
                a = a * a * (3 - 2 * a);
                for (int j = 0; j < 5; j++) out[j] = k[i][j + 1] + (k[i + 1][j + 1] - k[i][j + 1]) * a;
                return out;
            }
        }
        System.arraycopy(k[k.length - 1], 1, out, 0, 5);
        return out;
    }
}
