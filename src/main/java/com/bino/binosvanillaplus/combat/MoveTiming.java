package com.bino.binosvanillaplus.combat;


// Duracao de cada golpe e o momento do impacto (os mesmos numeros das animacoes, em client/combat/MoveAnims).
// Golpe LEVE dura pelo menos a recarga de ataque da arma (como no Better Combat): assim cada golpe sai com carga
// cheia (dano, critico e varrida normais do jogo) e uma arma mais rapida tem golpes mais rapidos.
public final class MoveTiming {
    private MoveTiming() {}

    static boolean isLight(CombatMove m, boolean heavy) { return !heavy && m != CombatMove.HEAVY; }

    // duracao em milissegundos; delayTicks = recarga de ataque da arma (Player.getCurrentItemAttackStrengthDelay)
    public static int lengthMs(CombatMove m, boolean heavy, boolean axe, float delayTicks) {
        int base = switch (m) {
            case LIGHT_1 -> 420; case LIGHT_2 -> 440; case LIGHT_3 -> 460;
            case HEAVY -> 760; case JUMP -> 560; case FORWARD -> 480; case FINISHER -> 900;
        };
        if (heavy && m != CombatMove.HEAVY) base += 220;   // versao pesada do pulo/investida/final: preparacao maior
        if (axe) base = (int) (base * 1.15f);
        if (isLight(m, heavy)) {
            int cd = (int) (delayTicks * 50f * (m == CombatMove.FINISHER ? 1.2f : 1f));
            base = Math.max(base, Math.min(cd, 1400));
        }
        return base;
    }

    // fracao da animacao em que a arma "bate"
    public static float impact(CombatMove m) {
        return switch (m) {
            case HEAVY -> 0.5f; case JUMP -> 0.55f; case FINISHER -> 0.62f; case FORWARD -> 0.32f;
            default -> 0.34f;
        };
    }

    public static int lengthTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.round(lengthMs(m, heavy, axe, delay) / 50f); }
    public static int windupTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.max(1, Math.round(lengthMs(m, heavy, axe, delay) * impact(m) / 50f)); }
}
