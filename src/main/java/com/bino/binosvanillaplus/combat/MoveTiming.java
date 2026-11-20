package com.bino.binosvanillaplus.combat;

// Duracao de cada golpe e o momento do impacto (os mesmos numeros das animacoes, em client/combat/MoveAnims).
// Golpe LEVE (ataque normal do jogo) dura pelo menos a recarga de ataque da arma (como no Better Combat): assim cada
// golpe sai com carga cheia (dano, critico e varrida normais) e uma arma mais rapida tem golpes mais rapidos.
public final class MoveTiming {
    private MoveTiming() {}

    // duracao em milissegundos; delayTicks = recarga de ataque da arma (Player.getCurrentItemAttackStrengthDelay)
    public static int lengthMs(CombatMove m, boolean heavy, boolean axe, float delayTicks) {
        int base = switch (m) {
            case LIGHT_1 -> 420; case LIGHT_2 -> 440; case LIGHT_3 -> 460; case LIGHT_4 -> 460;
            case HEAVY -> 760; case JUMP -> 560; case FORWARD -> 480;
            case TEMPEST -> 820; case JUDGMENT -> 1000; case GUARD_BREAK -> 640; case BREAK_1 -> 400; case BREAK_2 -> 560;
            case ROLL_SLASH -> 480; case ROLL_THRUST -> 620;
            case PARRY -> 320; case ISSEN -> 560; case RUSH -> 700; case CRITICAL -> 900;
        };
        if (heavy && (m == CombatMove.JUMP || m == CombatMove.FORWARD)) base += 220;   // versao pesada: preparacao maior
        if (axe && m != CombatMove.PARRY && m != CombatMove.ISSEN && m != CombatMove.RUSH) base = (int) (base * 1.15f);
        if (m.vanillaHit(heavy)) {
            int cd = (int) (delayTicks * 50f * (m == CombatMove.TEMPEST ? 1.2f : 1f));
            base = Math.max(base, Math.min(cd, 1400));
        }
        return base;
    }

    // fracao da animacao em que a arma "bate"
    public static float impact(CombatMove m) {
        return switch (m) {
            case HEAVY -> 0.5f; case JUMP -> 0.55f; case FORWARD -> 0.32f;
            case TEMPEST -> 0.6f; case JUDGMENT -> 0.66f; case GUARD_BREAK -> 0.45f;
            case ROLL_SLASH -> 0.3f; case ROLL_THRUST -> 0.4f;
            case PARRY -> 0.2f; case ISSEN -> 0.25f; case RUSH -> 0.1f; case CRITICAL -> 0.55f;
            default -> 0.34f;
        };
    }

    public static int lengthTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.round(lengthMs(m, heavy, axe, delay) / 50f); }
    public static int windupTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.max(1, Math.round(lengthMs(m, heavy, axe, delay) * impact(m) / 50f)); }
}
