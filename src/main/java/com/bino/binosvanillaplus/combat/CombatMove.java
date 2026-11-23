package com.bino.binosvanillaplus.combat;

// Golpes do hack and slash (mod Combat). Cada golpe tem uma animacao por arma (espada e machado, ver
// client/combat/MoveAnims) e um efeito no servidor (CombatMovesServer / CombatActionsServer).
//
// Regras de 23/11 (doc "Combate - Refinamento"): L = leve, P = pesado. O pesado SEMPRE fecha o combo e nao abre nenhum.
//   L L L L L = Base (LIGHT_1..LIGHT_5)      L L L L P = Tormenta (TORMENTA)     L L L P = Juizo (JUDGMENT)
//   L L P     = Quebra-Guarda (GUARD_BREAK)  L P       = Surpresa (SURPRISE)
//   No ar: os leves sao AIR_1..3 e o pesado que fecha e' sempre o METEOR. Abaixado (Shift): o pesado e' sempre o
//   LAUNCHER (Lanca-Ceu, levanta o inimigo). Fora da lista (contexto): DASH_SLASH / PIERCE (depois do avanco),
//   ROLL_SLASH / ROLL_THRUST (depois do rolamento), FORWARD (correndo). Todo pesado pode ser segurado: 4 niveis de
//   carga (instantaneo, fraco, medio, forte) multiplicam dano e postura (chargeMult).
//   PARRY, ISSEN, RUSH, CRITICAL, BACKSTAB: so animacao aqui; o efeito vem por CombatActionPayload.
//   HEAVY, JUMP, TEMPEST, BREAK_1, BREAK_2, DANCE: golpes antigos, fora das regras (ficam so por compatibilidade).
public enum CombatMove {
    LIGHT_1, LIGHT_2, LIGHT_3, LIGHT_4, HEAVY, JUMP, FORWARD,
    TEMPEST, JUDGMENT, GUARD_BREAK, BREAK_1, BREAK_2,
    ROLL_SLASH, ROLL_THRUST, PARRY, ISSEN, RUSH, CRITICAL,
    DASH, DASH_SLASH, PIERCE, LAUNCHER, AIR_1, AIR_2, AIR_3, METEOR, DANCE,
    BACKSTAB,   // agachado, por tras do mob: crava a arma nas costas (so animacao; o efeito vem por CombatActionPayload)
    LIGHT_5, TORMENTA, SURPRISE;

    public static final int MAX_COMBO = 5;

    public static CombatMove of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    // golpe que o SERVIDOR faz (arco a frente, empurrao forte) em vez do ataque normal do jogo
    public boolean serverHit(boolean heavy) {
        return switch (this) {
            case HEAVY, JUDGMENT, GUARD_BREAK, ROLL_THRUST, PIERCE, LAUNCHER, METEOR, DANCE, TORMENTA, SURPRISE -> true;
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
        return this == LIGHT_5 || this == TORMENTA || this == JUDGMENT || this == GUARD_BREAK || this == SURPRISE
                || this == LAUNCHER || this == METEOR || this == PIERCE || this == DASH_SLASH || this == ROLL_THRUST
                || this == TEMPEST || this == BREAK_2 || this == DANCE || this == HEAVY;
    }

    // pesado segurado: 0 instantaneo (ate 0,25 s), 1 fraco (ate 0,6 s), 2 medio (ate 1 s), 3 forte (ate 1,5 s, sai sozinho)
    public static final long[] CHARGE_MS = {250, 600, 1000, 1500};
    public static int chargeLevel(long heldMs) { return heldMs < 250 ? 0 : heldMs < 600 ? 1 : heldMs < 1000 ? 2 : 3; }
    public static float chargeMult(int level) { return switch (level) { case 1 -> 1.3f; case 2 -> 1.6f; case 3 -> 2.0f; default -> 1.0f; }; }

    // multiplicador do dano
    public float damage(boolean heavy, boolean axe) {
        return switch (this) {
            case LIGHT_1, LIGHT_2, LIGHT_3, LIGHT_4 -> 1.0f;
            case LIGHT_5 -> 1.15f;
            case SURPRISE -> axe ? 1.8f : 1.6f;
            case TORMENTA -> axe ? 2.8f : 2.5f;
            case BREAK_1, BREAK_2 -> 1.1f;
            case TEMPEST -> 1.6f;
            case ROLL_SLASH -> 1.3f;
            case HEAVY -> axe ? 2.1f : 1.7f;
            case JUMP -> heavy ? (axe ? 2.0f : 1.8f) : 1.25f;
            case FORWARD -> heavy ? (axe ? 1.9f : 1.75f) : 1.15f;
            case JUDGMENT -> axe ? 2.3f : 2.0f;
            case GUARD_BREAK -> axe ? 1.5f : 1.3f;
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
            case TORMENTA -> 2.4f;
            case SURPRISE -> 1.8f;
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
