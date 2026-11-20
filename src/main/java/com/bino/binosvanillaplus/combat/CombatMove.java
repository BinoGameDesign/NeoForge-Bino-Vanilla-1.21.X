package com.bino.binosvanillaplus.combat;

// Golpes do hack and slash (mod Combat). Cada golpe tem uma animacao por arma (espada e machado, ver
// client/combat/MoveAnims) e um efeito no servidor (CombatMovesServer).
//   LIGHT_1..3 = golpes leves (botao esquerdo) alternando no combo
//   HEAVY      = golpe pesado (botao direito em combate)
//   JUMP       = golpe no ar (pulando)
//   FORWARD    = golpe para a frente (correndo: estocada/investida)
//   FINISHER   = 5o golpe do combo (o mais forte)
public enum CombatMove {
    LIGHT_1, LIGHT_2, LIGHT_3, HEAVY, JUMP, FORWARD, FINISHER;

    public static final int MAX_COMBO = 5;

    public static CombatMove of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    // multiplicador do dano do golpe (leve usa o golpe normal do jogo com esse bonus; pesado faz o proprio golpe)
    public float damage(boolean heavy, boolean axe) {
        return switch (this) {
            case HEAVY -> axe ? 2.1f : 1.7f;
            case JUMP -> heavy ? (axe ? 2.0f : 1.8f) : 1.25f;
            case FORWARD -> heavy ? (axe ? 1.9f : 1.75f) : 1.15f;
            case FINISHER -> heavy ? (axe ? 2.6f : 2.2f) : 1.6f;
            default -> 1.0f;
        };
    }
}
