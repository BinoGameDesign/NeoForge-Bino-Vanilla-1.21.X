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
// Combos de movimento (fase 4 do dossie):
//   W W = DASH (avanco); logo depois: L = DASH_SLASH (Avanco), P = PIERCE (Perfurador)
//   L L + P SEGURADO = LAUNCHER (Lanca-Ceu: joga o mob para cima e voce pula atras)
//   no ar: L L L = AIR_1..3 (Combo Aereo, flutuando); P depois de um golpe no ar = METEOR (cai em cima)
//   R R P (dois rolamentos e pesado) = DANCE (giro em volta)
public enum CombatMove {
    LIGHT_1, LIGHT_2, LIGHT_3, LIGHT_4, HEAVY, JUMP, FORWARD,
    TEMPEST, JUDGMENT, GUARD_BREAK, BREAK_1, BREAK_2,
    ROLL_SLASH, ROLL_THRUST, PARRY, ISSEN, RUSH, CRITICAL,
    DASH, DASH_SLASH, PIERCE, LAUNCHER, AIR_1, AIR_2, AIR_3, METEOR, DANCE,
    BACKSTAB;   // agachado, por tras do mob: crava a arma nas costas (so animacao; o efeito vem por CombatActionPayload)

    public static final int MAX_COMBO = 5;

    public static CombatMove of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    // golpe que o SERVIDOR faz (arco a frente, empurrao forte) em vez do ataque normal do jogo
    public boolean serverHit(boolean heavy) {
        return switch (this) {
            case HEAVY, JUDGMENT, GUARD_BREAK, ROLL_THRUST, PIERCE, LAUNCHER, METEOR, DANCE -> true;
            case JUMP, FORWARD -> heavy;
            default -> false;
        };
    }

    // so animacao (o efeito vem de outro lugar)
    public boolean visualOnly() { return this == PARRY || this == ISSEN || this == RUSH || this == CRITICAL || this == DASH || this == BACKSTAB; }

    public boolean aerial() { return this == AIR_1 || this == AIR_2 || this == AIR_3; }

    // golpe leve que usa o ataque normal do jogo no impacto
    public boolean vanillaHit(boolean heavy) { return !serverHit(heavy) && !visualOnly(); }

    // fecha o combo (o proximo aperto comeca do zero)
    public boolean closesCombo() {
        return this == TEMPEST || this == JUDGMENT || this == BREAK_2 || this == LAUNCHER || this == AIR_3 || this == METEOR
                || this == DANCE || this == PIERCE || this == DASH_SLASH;
    }

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
            case DASH_SLASH -> 1.35f;
            case PIERCE -> axe ? 2.0f : 1.7f;
            case LAUNCHER -> axe ? 1.5f : 1.3f;
            case AIR_3 -> 1.3f;
            case METEOR -> axe ? 2.4f : 2.0f;
            case DANCE -> axe ? 1.9f : 1.6f;
            default -> 1.0f;
        };
    }

    // quanto o golpe enche a postura do alvo (vezes o dano)
    public float posture() {
        return switch (this) {
            case GUARD_BREAK -> 4.0f;
            case LAUNCHER -> 2.5f;
            case METEOR -> 2.2f;
            case PIERCE -> 1.8f;
            case DANCE -> 1.6f;
            case AIR_3 -> 1.5f;
            case JUDGMENT, HEAVY -> 2.0f;
            case TEMPEST, BREAK_2 -> 1.8f;
            case JUMP, FORWARD, ROLL_THRUST -> 1.5f;
            default -> 1.0f;
        };
    }
}
