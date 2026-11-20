package com.bino.binosvanillaplus.combat;

// Golpes do hack and slash (mod Combat). Cada golpe tem uma animacao por arma (espada e machado, ver
// client/combat/MoveAnims) e um efeito no servidor (CombatMovesServer / CombatActionsServer).
//
// Arvore de combos (L = leve/esquerdo, P = pesado/direito; o cliente escolhe o galho em CombatMoves):
//   L L L L L = TEMPESTADE (espada) / LENHADOR (machado)   -> LIGHT_1 LIGHT_2 LIGHT_3 LIGHT_4 TEMPEST
//   L L L L P = JUIZO                                       -> LIGHT_1 LIGHT_2 LIGHT_3 LIGHT_4 JUDGMENT
//   L L P L L = QUEBRA-GUARDA                               -> LIGHT_1 LIGHT_2 GUARD_BREAK BREAK_1 BREAK_2
//   P sozinho = HEAVY. No ar = JUMP, correndo = FORWARD, logo depois de rolar = ROLL_SLASH (L) / ROLL_THRUST (P).
//   PARRY, ISSEN (contra-golpe do parry perfeito), RUSH (rajada da esquiva perfeita), CRITICAL (guarda quebrada):
//   so animacao aqui; o efeito vem por CombatActionPayload.
public enum CombatMove {
    LIGHT_1, LIGHT_2, LIGHT_3, LIGHT_4, HEAVY, JUMP, FORWARD,
    TEMPEST, JUDGMENT, GUARD_BREAK, BREAK_1, BREAK_2,
    ROLL_SLASH, ROLL_THRUST, PARRY, ISSEN, RUSH, CRITICAL;

    public static final int MAX_COMBO = 5;

    public static CombatMove of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    // golpe que o SERVIDOR faz (arco a frente, empurrao forte) em vez do ataque normal do jogo
    public boolean serverHit(boolean heavy) {
        return switch (this) {
            case HEAVY, JUDGMENT, GUARD_BREAK, ROLL_THRUST -> true;
            case JUMP, FORWARD -> heavy;
            default -> false;
        };
    }

    // so animacao (o efeito vem de outro lugar)
    public boolean visualOnly() { return this == PARRY || this == ISSEN || this == RUSH || this == CRITICAL; }

    // golpe leve que usa o ataque normal do jogo no impacto
    public boolean vanillaHit(boolean heavy) { return !serverHit(heavy) && !visualOnly(); }

    // fecha o combo (o proximo aperto comeca do zero)
    public boolean closesCombo() { return this == TEMPEST || this == JUDGMENT || this == BREAK_2; }

    // multiplicador do dano
    public float damage(boolean heavy, boolean axe) {
        return switch (this) {
            case LIGHT_1, LIGHT_2, LIGHT_3, LIGHT_4 -> 1.0f;
            case BREAK_1, BREAK_2 -> 1.1f;
            case TEMPEST -> 1.6f;
            case ROLL_SLASH -> 1.3f;
            case HEAVY -> axe ? 2.1f : 1.7f;
            case JUMP -> heavy ? (axe ? 2.0f : 1.8f) : 1.25f;
            case FORWARD -> heavy ? (axe ? 1.9f : 1.75f) : 1.15f;
            case JUDGMENT -> axe ? 2.8f : 2.4f;
            case GUARD_BREAK -> axe ? 1.4f : 1.2f;
            case ROLL_THRUST -> axe ? 1.8f : 1.6f;
            default -> 1.0f;
        };
    }

    // quanto o golpe enche a postura do alvo (vezes o dano)
    public float posture() {
        return switch (this) {
            case GUARD_BREAK -> 4.0f;
            case JUDGMENT, HEAVY -> 2.0f;
            case TEMPEST, BREAK_2 -> 1.8f;
            case JUMP, FORWARD, ROLL_THRUST -> 1.5f;
            default -> 1.0f;
        };
    }
}
