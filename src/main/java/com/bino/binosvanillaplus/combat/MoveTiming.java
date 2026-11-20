package com.bino.binosvanillaplus.combat;

// Duracao de cada golpe e o momento do impacto (os mesmos numeros das animacoes, em client/combat/MoveAnims)
public final class MoveTiming {
    private MoveTiming() {}

    // duracao em milissegundos
    public static int lengthMs(CombatMove m, boolean heavy, boolean axe) {
        int base = switch (m) {
            case LIGHT_1 -> 300; case LIGHT_2 -> 320; case LIGHT_3 -> 340;
            case HEAVY -> 650; case JUMP -> 460; case FORWARD -> 360; case FINISHER -> 720;
        };
        if (heavy && m != CombatMove.HEAVY) base += 180;   // versao pesada do pulo/investida/final: preparacao maior
        return axe ? (int) (base * 1.25f) : base;
    }

    // fracao da animacao em que a arma "bate"
    public static float impact(CombatMove m) {
        return switch (m) {
            case HEAVY -> 0.5f; case JUMP -> 0.55f; case FINISHER -> 0.62f; case FORWARD -> 0.32f;
            default -> 0.34f;
        };
    }

    public static int lengthTicks(CombatMove m, boolean heavy, boolean axe) { return Math.round(lengthMs(m, heavy, axe) / 50f); }
    public static int windupTicks(CombatMove m, boolean heavy, boolean axe) { return Math.max(1, Math.round(lengthMs(m, heavy, axe) * impact(m) / 50f)); }
}
