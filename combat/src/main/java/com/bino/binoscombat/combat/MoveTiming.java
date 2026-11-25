package com.bino.binoscombat.combat;

// Duracao de cada golpe e o momento do impacto (os mesmos numeros das animacoes, em client/combat/MoveAnims).
// Golpe LEVE (ataque normal do jogo) dura pelo menos a recarga de ataque da arma (como no Better Combat): assim cada
// golpe sai com carga cheia (dano, critico e varrida normais) e uma arma mais rapida tem golpes mais rapidos.
public final class MoveTiming {
    private MoveTiming() {}

    // duracao em milissegundos; delayTicks = recarga de ataque da arma (Player.getCurrentItemAttackStrengthDelay)
    public static int lengthMs(CombatMove m, boolean heavy, boolean axe, float delayTicks) {
        BbTiming.Times bb = BbTiming.get(m, axe);
        if (bb != null) {   // animacao do Blockbench: a duracao dela, mais rapida/lenta com a velocidade de ataque da arma
            boolean fixed = m == CombatMove.PARRY || m == CombatMove.ISSEN || m == CombatMove.RUSH || m == CombatMove.DASH || m == CombatMove.BACKSTAB;
            return Math.round(bb.length() * 1000f * (fixed ? 1f : speedScale(delayTicks)));
        }
        int base = switch (m) {
            case LIGHT_1 -> 420; case LIGHT_2 -> 440; case LIGHT_3 -> 460; case LIGHT_4 -> 460;
            case HEAVY -> 760; case JUMP -> 560; case FORWARD -> 480;
            case TEMPEST -> 820; case JUDGMENT -> 1000; case GUARD_BREAK -> 640; case BREAK_1 -> 400; case BREAK_2 -> 560;
            case ROLL_SLASH -> 480; case ROLL_THRUST -> 620;
            case PARRY -> 320; case ISSEN -> 560; case RUSH -> 700; case CRITICAL -> 900;
            case DASH -> 300; case DASH_SLASH -> 460; case PIERCE -> 620; case LAUNCHER -> 600;
            case AIR_1 -> 380; case AIR_2 -> 380; case AIR_3 -> 480; case METEOR -> 700; case DANCE -> 820;
            case BACKSTAB -> 1300;
            case LIGHT_5 -> 520; case TORMENTA -> 900; case SURPRISE -> 640;
        };
        if (heavy && (m == CombatMove.JUMP || m == CombatMove.FORWARD)) base += 220;   // versao pesada: preparacao maior
        // velocidade de ataque da arma muda a velocidade da animacao (espada, 1,6 ataques/s = 12,5 ticks, e' a base)
        if (m != CombatMove.PARRY && m != CombatMove.ISSEN && m != CombatMove.RUSH && m != CombatMove.DASH && m != CombatMove.BACKSTAB)
            base = (int) (base * speedScale(delayTicks));
        if (m.vanillaHit(heavy)) {
            int cd = (int) (delayTicks * 50f * (m == CombatMove.TEMPEST ? 1.2f : 1f));
            base = Math.max(base, Math.min(cd, 1400));
        }
        return base;
    }

    public static float speedScale(float delayTicks) {
        if (delayTicks <= 0) return 1f;
        return (float) Math.max(0.65, Math.min(1.7, Math.pow(delayTicks / 12.5, 0.6)));
    }

    // fracao da animacao em que a arma "bate" (animacao do Blockbench: o marcador da linha do tempo)
    public static float impact(CombatMove m, boolean axe) {
        BbTiming.Times bb = BbTiming.get(m, axe);
        return bb != null ? bb.hit() / bb.length() : impact(m);
    }

    // a partir de que fracao o proximo golpe do combo pode sair (Blockbench: depois da pausa do acerto)
    public static float chain(CombatMove m, boolean heavy, boolean axe) {
        BbTiming.Times bb = BbTiming.get(m, axe);
        if (bb != null) return bb.chain() / bb.length();
        return m.serverHit(heavy) ? 0.9f : 0.84f;
    }

    public static float impact(CombatMove m) {
        return switch (m) {
            case HEAVY -> 0.5f; case JUMP -> 0.55f; case FORWARD -> 0.32f;
            case TEMPEST -> 0.6f; case JUDGMENT -> 0.66f; case GUARD_BREAK -> 0.45f;
            case ROLL_SLASH -> 0.3f; case ROLL_THRUST -> 0.4f;
            case PARRY -> 0.2f; case ISSEN -> 0.25f; case RUSH -> 0.1f; case CRITICAL -> 0.55f;
            case DASH -> 0.5f; case DASH_SLASH -> 0.3f; case PIERCE -> 0.4f; case LAUNCHER -> 0.45f;
            case AIR_3 -> 0.4f; case METEOR -> 0.45f; case DANCE -> 0.55f; case BACKSTAB -> 0.36f;
            case TORMENTA -> 0.6f; case SURPRISE -> 0.4f; case LIGHT_5 -> 0.36f;
            default -> 0.34f;
        };
    }

    public static int lengthTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.round(lengthMs(m, heavy, axe, delay) / 50f); }
    public static int windupTicks(CombatMove m, boolean heavy, boolean axe, float delay) { return Math.max(1, Math.round(lengthMs(m, heavy, axe, delay) * impact(m, axe) / 50f)); }
}
