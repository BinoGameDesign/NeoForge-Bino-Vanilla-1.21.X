package com.bino.binosvanillaplus.client.inventory;

// Poses de acao do personagem no inventario novo (uma por aba). Angulos em radianos.
// Cada braco/perna tem duas partes (cotovelo/joelho): "up" = parte de cima, "low" = dobra.
// Cotovelo: dobra para a frente com valor negativo. Joelho: dobra para tras com valor positivo.
public final class ActionPose {
    private ActionPose() {}

    public enum Pose { GUARD, HERO, STRIKE }

    // {x, y, z} de cada osso: cabeca, corpo, braco D (cima, dobra), braco E (cima, dobra), perna D (cima, dobra), perna E (cima, dobra)
    public record Angles(float[] head, float[] body, float[] rArm, float rElbow, float[] lArm, float lElbow,
                         float[] rLeg, float rKnee, float[] lLeg, float lKnee) {}

    private static float[] v(float x, float y, float z) { return new float[]{x, y, z}; }

    public static Angles of(Pose p) {
        return switch (p) {
            // em guarda: espada para a frente e para fora, cotovelo dobrado, base aberta com joelhos flexionados
            case GUARD -> new Angles(v(0.08f, -0.25f, 0f), v(0f, -0.15f, 0f),
                    v(-0.45f, 0.35f, 0.55f), -0.95f, v(0.35f, 0f, -0.30f), -0.45f,
                    v(0.40f, 0f, 0.12f), 0.55f, v(-0.45f, 0f, -0.10f), 0.30f);
            // heroica: item erguido acima da cabeca, a outra mao na cintura, peito aberto
            case HERO -> new Angles(v(-0.25f, 0.10f, 0f), v(0f, 0.05f, 0f),
                    v(-2.80f, 0.05f, 0.15f), -0.30f, v(-0.20f, 0.30f, -0.75f), -1.55f,
                    v(0.05f, 0f, 0.16f), 0.10f, v(-0.10f, 0f, -0.16f), 0.20f);
            // golpe: estocada para a frente, passo largo
            case STRIKE -> new Angles(v(0.05f, 0.30f, 0f), v(0f, 0.28f, 0f),
                    v(-1.70f, -0.30f, 0.05f), -0.20f, v(0.60f, 0f, -0.15f), -0.80f,
                    v(-0.75f, 0f, 0.05f), 0.95f, v(0.55f, 0f, -0.05f), 0.25f);
        };
    }
}
