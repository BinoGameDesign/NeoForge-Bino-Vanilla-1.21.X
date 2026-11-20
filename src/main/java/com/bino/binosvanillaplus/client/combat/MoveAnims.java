package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.combat.CombatMove;

// Animacoes dos golpes (mod Combat): uma por golpe e por arma (espada e machado, sempre uma mao so).
//
// Cada chave descreve o CORPO INTEIRO:
//   mao (vista da camera):  hx, hy, hz = posicao (x direita, y cima, z negativo = frente)
//                           ay = ponta para o lado (+ direita), ap = de cima (0 ponta para cima, 90 frente, 180 baixo),
//                           er = giro da lamina no proprio eixo (90 = fio de lado, cortes horizontais)
//   corpo:  tw = tronco torce (+ direita), ln = inclina para a frente, sp = rodopio (giro em pe, 360 = volta inteira),
//           rl = tomba de lado (estrela/parafuso), fl = cambalhota (+ frente, - salto mortal para tras),
//           hop = pulinho (so visual), st = passo (perna da frente), cr = agacha/joelhos (0..1), dx = corpo vai a frente
// Ordem do golpe: pe -> quadril -> tronco -> ombro -> braco -> arma (os principios estao no Dossie do Combate).
// Curva do trecho que TERMINA na chave: W preparacao (suave), S golpe (acelera ate o impacto), F continuacao (freia),
// R volta ao descanso. Primeira pessoa usa a mao + camera (inclina, abaixa, rola); terceira pessoa usa tudo.
public final class MoveAnims {
    private MoveAnims() {}

    static final int HX = 0, HY = 1, HZ = 2, AY = 3, AP = 4, ER = 5, TW = 6, LN = 7, SP = 8, RL = 9, FL = 10,
            HOP = 11, ST = 12, CR = 13, DX = 14, N = 15;
    static final int LIN = 0, W = 1, S = 2, F = 3, R = 4;

    // descanso (mao parada com a arma)
    static final float[] REST = {0.50f, -0.50f, -0.72f, -8, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    record Key(float t, int ease, float[] v) {}

    // construtor das chaves: k(t, curva).h(mao).a(lamina).b(tronco, inclinacao).r(rodopio, tombo, cambalhota).m(pulo, passo, agacha).d(frente)
    static final class K {
        final float t; final int ease; final float[] v = REST.clone();
        K(float t, int ease) { this.t = t; this.ease = ease; }
        K h(float x, float y, float z) { v[HX] = x; v[HY] = y; v[HZ] = z; return this; }
        K a(float ay, float ap, float er) { v[AY] = ay; v[AP] = ap; v[ER] = er; return this; }
        K b(float tw, float ln) { v[TW] = tw; v[LN] = ln; return this; }
        K r(float sp, float rl, float fl) { v[SP] = sp; v[RL] = rl; v[FL] = fl; return this; }
        K m(float hop, float st, float cr) { v[HOP] = hop; v[ST] = st; v[CR] = cr; return this; }
        K d(float dx) { v[DX] = dx; return this; }
        Key key() { return new Key(t, ease, v); }
    }

    private static K k(float t, int ease) { return new K(t, ease); }
    private static K rest(float t) { return new K(t, R); }
    private static K rest(float t, float spin, float flip) { return new K(t, R).r(spin, 0, flip); }

    private static Key[] keys(K... ks) {
        Key[] out = new Key[ks.length + 1];
        out[0] = new Key(0, LIN, REST.clone());
        for (int i = 0; i < ks.length; i++) out[i + 1] = ks[i].key();
        return out;
    }

    static Key[] keys(CombatMove m, boolean axe) { return axe ? axe(m) : sword(m); }

    // ================================================================== ESPADA: leve, rapida, giros e saltos
    private static Key[] sword(CombatMove m) {
        return switch (m) {
            // diagonal de cima-direita para baixo-esquerda, pisando no golpe
            case LIGHT_1 -> keys(
                    k(0.20f, W).h(0.66f, 0.08f, -0.42f).a(55, 15, -35).b(30, -4).m(0, 0, 0.2f),
                    k(0.34f, S).h(0.02f, -0.30f, -0.84f).a(-30, 110, -60).b(-10, 12).m(0, 0.45f, 0.35f).d(0.15f),
                    k(0.52f, F).h(-0.44f, -0.56f, -0.62f).a(-78, 142, -60).b(-32, 14).m(0, 0.45f, 0.3f).d(0.2f),
                    rest(1));
            // reves horizontal da esquerda para a direita, o corpo gira junto
            case LIGHT_2 -> keys(
                    k(0.22f, W).h(-0.44f, -0.24f, -0.46f).a(-100, 84, 90).b(-38, 0).r(0, -6, 0).m(0, 0.1f, 0.15f),
                    k(0.34f, S).h(0.04f, -0.28f, -0.88f).a(-4, 88, 90).b(0, 6).m(0, 0.35f, 0.3f).d(0.12f),
                    k(0.54f, F).h(0.58f, -0.32f, -0.50f).a(85, 92, 90).b(38, 8).r(0, 6, 0).m(0, 0.35f, 0.25f).d(0.15f),
                    rest(1));
            // corte subindo (de baixo-esquerda para cima-direita)
            case LIGHT_3 -> keys(
                    k(0.22f, W).h(-0.30f, -0.62f, -0.45f).a(-40, 160, 180).b(-25, 14).m(0, 0.2f, 0.45f),
                    k(0.34f, S).h(0.10f, -0.10f, -0.88f).a(10, 70, 180).b(5, 0).m(0, 0.5f, 0.2f).d(0.15f),
                    k(0.54f, F).h(0.46f, 0.22f, -0.55f).a(50, 15, 180).b(25, -10).m(0, 0.5f, 0),
                    rest(1));
            // pirueta: gira 360 com a lamina aberta
            case LIGHT_4 -> keys(
                    k(0.15f, W).h(0.66f, -0.10f, -0.32f).a(92, 88, -90).b(40, 0).m(0, 0.2f, 0.25f),
                    k(0.34f, S).h(-0.10f, -0.26f, -0.86f).a(-20, 88, -90).b(-10, 6).r(200, 0, 0).m(0, 0.3f, 0.3f).d(0.15f),
                    k(0.55f, F).h(-0.56f, -0.28f, -0.52f).a(-92, 88, -90).b(-40, 6).r(360, 0, 0).m(0, 0.3f, 0.2f),
                    rest(1, 360, 0));
            // TEMPESTADE (5o): salto em parafuso e corte caindo do outro lado
            case TEMPEST -> keys(
                    k(0.18f, W).h(0.62f, 0.0f, -0.30f).a(80, 60, -60).b(40, -6).m(0, 0.2f, 0.5f),
                    k(0.40f, LIN).h(0.40f, 0.30f, -0.40f).a(40, 0, -40).b(10, -10).r(180, -25, 0).m(0.25f, 0, 0),
                    k(0.60f, S).h(-0.05f, -0.38f, -0.88f).a(-30, 125, -50).b(-15, 25).r(360, 0, 0).m(0.1f, 0.6f, 0.3f).d(0.3f),
                    k(0.80f, F).h(-0.42f, -0.60f, -0.62f).a(-70, 155, -50).b(-30, 25).r(360, 0, 0).m(0, 0.6f, 0.5f),
                    rest(1, 360, 0));
            // JUIZO (5o pesado): salto mortal para tras e estocada aerea
            case JUDGMENT -> keys(
                    k(0.15f, W).h(0.40f, -0.30f, -0.30f).a(0, 60, 90).b(0, -8).m(0, 0, 0.5f),
                    k(0.45f, LIN).h(0.20f, 0.30f, -0.20f).a(0, -30, 0).b(0, -20).r(0, 0, -300).m(0.3f, 0, 0),
                    k(0.58f, W).h(0.15f, 0.35f, -0.25f).a(0, -20, 90).r(0, 0, -360).m(0.2f, 0, 0),
                    k(0.66f, S).h(0.10f, -0.28f, -1.15f).a(-3, 100, 90).b(-10, 28).r(0, 0, -360).m(0, 0.8f, 0.3f).d(0.4f),
                    k(0.84f, F).h(0.10f, -0.30f, -1.05f).a(-3, 100, 90).b(-10, 28).r(0, 0, -360).m(0, 0.8f, 0.3f),
                    rest(1, 0, -360));
            // QUEBRA-GUARDA (3o): estocada com o ombro que atordoa
            case GUARD_BREAK -> keys(
                    k(0.30f, W).h(0.30f, -0.40f, -0.15f).a(-5, 88, 90).b(35, -5).m(0, 0, 0.4f),
                    k(0.45f, S).h(0.15f, -0.28f, -1.10f).a(-3, 90, 90).b(-25, 22).m(0, 0.9f, 0.4f).d(0.35f),
                    k(0.65f, F).h(0.15f, -0.30f, -1.02f).a(-3, 90, 90).b(-22, 20).m(0, 0.9f, 0.4f).d(0.3f),
                    rest(1));
            // 4o do Quebra-Guarda: primeiro corte do X
            case BREAK_1 -> keys(
                    k(0.20f, W).h(0.50f, 0.15f, -0.40f).a(40, 20, -40).b(20, 0).m(0, 0.2f, 0.2f),
                    k(0.34f, S).h(-0.20f, -0.40f, -0.80f).a(-50, 130, -40).b(-15, 10).m(0, 0.3f, 0.3f).d(0.1f),
                    k(0.50f, F).h(-0.35f, -0.50f, -0.60f).a(-70, 145, -40).b(-20, 12).m(0, 0.3f, 0.3f),
                    rest(1));
            // 5o do Quebra-Guarda: o outro corte do X e pose final
            case BREAK_2 -> keys(
                    k(0.20f, W).h(-0.45f, 0.12f, -0.40f).a(-40, 20, 40).b(-20, 0).m(0, 0.2f, 0.2f),
                    k(0.34f, S).h(0.25f, -0.40f, -0.80f).a(50, 130, 40).b(15, 10).m(0, 0.35f, 0.3f).d(0.12f),
                    k(0.55f, F).h(0.45f, -0.55f, -0.55f).a(75, 150, 40).b(25, 14).m(0, 0.35f, 0.4f),
                    rest(1));
            // saindo do rolamento: corte baixo girando que sobe
            case ROLL_SLASH -> keys(
                    k(0.12f, W).h(0.55f, -0.65f, -0.35f).a(90, 120, -80).b(40, 10).m(0, 0, 0.7f),
                    k(0.30f, S).h(-0.05f, -0.35f, -0.85f).a(-20, 90, -80).b(-5, 8).r(180, 0, 0).m(0, 0.3f, 0.5f).d(0.1f),
                    k(0.50f, F).h(-0.45f, 0.05f, -0.55f).a(-80, 40, -80).b(-35, -5).r(360, 0, 0).m(0, 0.3f, 0.1f),
                    rest(1, 360, 0));
            // saindo do rolamento pesado: estocada longa
            case ROLL_THRUST -> keys(
                    k(0.25f, W).h(0.42f, -0.36f, -0.20f).a(-4, 88, 90).b(25, -6).m(0, 0, 0.5f),
                    k(0.40f, S).h(0.12f, -0.24f, -1.20f).a(-3, 90, 90).b(-18, 25).m(0, 1, 0.45f).d(0.5f),
                    k(0.62f, F).h(0.12f, -0.26f, -1.10f).a(-3, 90, 90).b(-16, 24).m(0, 1, 0.45f).d(0.45f),
                    rest(1));
            // pesado: puxa por tras do ombro, segura e desce com o corpo todo
            case HEAVY -> keys(
                    k(0.36f, W).h(0.72f, 0.22f, -0.18f).a(75, -12, -20).b(45, -8).m(0, 0.25f, 0.3f),
                    k(0.44f, LIN).h(0.74f, 0.26f, -0.15f).a(78, -16, -20).b(48, -10).m(0, 0.25f, 0.35f),
                    k(0.50f, S).h(-0.10f, -0.36f, -0.90f).a(-35, 122, -50).b(-20, 22).m(0, 0.7f, 0.55f).d(0.2f),
                    k(0.66f, F).h(-0.52f, -0.62f, -0.55f).a(-82, 152, -50).b(-40, 26).m(0, 0.7f, 0.5f),
                    rest(1));
            // no ar: cambalhota para a frente e crava
            case JUMP -> keys(
                    k(0.30f, W).h(0.24f, 0.36f, -0.24f).a(0, -42, 0).b(0, -16).r(0, 0, 160).m(0.25f, 0, 0),
                    k(0.55f, S).h(0.05f, -0.40f, -0.86f).a(-2, 136, 0).b(0, 30).r(0, 0, 360).m(0, 0.3f, 0.3f),
                    k(0.72f, F).h(0.05f, -0.56f, -0.74f).a(-2, 160, 0).b(0, 30).r(0, 0, 360).m(0, 0.3f, 0.4f),
                    rest(1, 0, 360));
            // investida: estocada reta deslizando
            case FORWARD -> keys(
                    k(0.20f, W).h(0.42f, -0.36f, -0.24f).a(-4, 88, 90).b(22, -6).m(0, 0, 0.4f),
                    k(0.32f, S).h(0.12f, -0.25f, -1.16f).a(-3, 90, 90).b(-16, 22).m(0, 0.9f, 0.35f).d(0.4f),
                    k(0.52f, F).h(0.12f, -0.26f, -1.05f).a(-3, 90, 90).b(-16, 22).m(0, 0.9f, 0.35f).d(0.35f),
                    rest(1));
            // parry: lamina atravessada na frente do rosto, desvia e abre
            case PARRY -> keys(
                    k(0.20f, S).h(0.05f, -0.15f, -0.55f).a(-75, 80, 90).b(-10, -6).m(0, 0.2f, 0.2f),
                    k(0.45f, F).h(0.25f, -0.05f, -0.55f).a(-40, 40, 90).b(10, -8).m(0, 0.2f, 0.15f),
                    rest(1));
            // Issen: saque relampago atravessando o inimigo
            case ISSEN -> keys(
                    k(0.10f, W).h(-0.30f, -0.40f, -0.25f).a(-80, 100, -90).b(-35, 5).m(0, 0.2f, 0.6f),
                    k(0.25f, S).h(0.60f, -0.30f, -0.50f).a(90, 90, -90).b(40, 15).m(0, 0.9f, 0.5f).d(0.6f),
                    k(0.55f, F).h(0.62f, -0.40f, -0.45f).a(100, 100, -90).b(42, 15).m(0, 0.9f, 0.5f).d(0.5f),
                    rest(1));
            // Rajada: cortes cruzados rapidos e um final de cima
            case RUSH -> keys(
                    k(0.06f, S).h(0.50f, 0.0f, -0.50f).a(60, 40, -40).b(25, 0).m(0, 0.3f, 0.2f),
                    k(0.16f, S).h(-0.40f, -0.40f, -0.70f).a(-60, 130, -40).b(-25, 8).m(0, 0.3f, 0.3f),
                    k(0.26f, S).h(-0.40f, 0.0f, -0.60f).a(-60, 40, 40).b(-25, 0).m(0, 0.3f, 0.2f),
                    k(0.36f, S).h(0.40f, -0.40f, -0.70f).a(60, 130, 40).b(25, 8).m(0, 0.3f, 0.3f),
                    k(0.46f, S).h(0.0f, 0.20f, -0.60f).a(0, -10, 0).b(0, -10).m(0, 0.3f, 0.1f),
                    k(0.56f, S).h(0.05f, -0.45f, -0.85f).a(0, 140, 0).b(0, 20).m(0, 0.5f, 0.3f),
                    k(0.75f, F).h(0.05f, -0.50f, -0.80f).a(0, 148, 0).b(0, 22).m(0, 0.5f, 0.35f),
                    rest(1));
            // golpe critico: ergue e crava
            case CRITICAL -> keys(
                    k(0.30f, W).h(0.30f, 0.25f, -0.20f).a(5, -30, 90).b(15, -10).m(0, 0.3f, 0.2f),
                    k(0.55f, S).h(0.10f, -0.45f, -1.0f).a(-3, 120, 90).b(-5, 30).m(0, 1, 0.6f).d(0.3f),
                    k(0.80f, F).h(0.10f, -0.48f, -0.98f).a(-3, 124, 90).b(-5, 30).m(0, 1, 0.6f).d(0.3f),
                    rest(1));
        };
    }

    // ================================================================== MACHADO: pesado, pes plantados, quedas
    private static Key[] axe(CombatMove m) {
        return switch (m) {
            // machadada por cima do ombro
            case LIGHT_1 -> keys(
                    k(0.26f, W).h(0.56f, 0.30f, -0.28f).a(36, -24, -10).b(32, -8).m(0, 0.1f, 0.2f),
                    k(0.36f, S).h(0.02f, -0.32f, -0.86f).a(-20, 126, -15).b(-6, 18).m(0, 0.5f, 0.45f).d(0.1f),
                    k(0.56f, F).h(-0.30f, -0.66f, -0.55f).a(-42, 166, -15).b(-22, 22).m(0, 0.5f, 0.5f),
                    rest(1));
            // gancho baixo da esquerda para a direita
            case LIGHT_2 -> keys(
                    k(0.26f, W).h(-0.46f, -0.22f, -0.40f).a(-104, 80, 90).b(-42, 4).m(0, 0.2f, 0.4f),
                    k(0.36f, S).h(0.05f, -0.32f, -0.86f).a(0, 86, 90).b(0, 10).m(0, 0.4f, 0.45f).d(0.1f),
                    k(0.56f, F).h(0.62f, -0.30f, -0.44f).a(98, 90, 90).b(42, 8).m(0, 0.4f, 0.35f),
                    rest(1));
            // de baixo para cima
            case LIGHT_3 -> keys(
                    k(0.26f, W).h(0.46f, -0.66f, -0.40f).a(30, 166, 180).b(26, 14).m(0, 0.2f, 0.55f),
                    k(0.36f, S).h(0.0f, -0.06f, -0.86f).a(-10, 60, 180).b(-10, -4).m(0, 0.4f, 0.15f),
                    k(0.56f, F).h(-0.30f, 0.26f, -0.54f).a(-40, 10, 180).b(-26, -12).m(0.1f, 0.4f, 0),
                    rest(1));
            // horizontal na altura do ombro, da direita para a esquerda
            case LIGHT_4 -> keys(
                    k(0.26f, W).h(0.64f, -0.05f, -0.30f).a(88, 85, -90).b(46, 4).m(0, 0.2f, 0.35f),
                    k(0.36f, S).h(-0.05f, -0.28f, -0.92f).a(-10, 88, -90).b(-10, 14).m(0, 0.45f, 0.4f).d(0.12f),
                    k(0.56f, F).h(-0.56f, -0.32f, -0.52f).a(-92, 90, -90).b(-46, 14).m(0, 0.45f, 0.3f),
                    rest(1));
            // LENHADOR (5o): giro de 360 com o machado aberto
            case TEMPEST -> keys(
                    k(0.18f, W).h(0.66f, -0.20f, -0.30f).a(92, 85, -90).b(46, 4).m(0, 0.2f, 0.45f),
                    k(0.42f, LIN).h(-0.56f, -0.30f, -0.50f).a(-92, 85, -90).b(-46, 6).r(240, 0, 0).m(0, 0.3f, 0.4f),
                    k(0.60f, S).h(-0.05f, -0.30f, -0.90f).a(-10, 88, -90).b(-5, 10).r(360, 0, 0).m(0, 0.4f, 0.45f).d(0.15f),
                    k(0.80f, F).h(-0.56f, -0.32f, -0.52f).a(-92, 90, -90).b(-46, 10).r(360, 0, 0).m(0, 0.4f, 0.4f),
                    rest(1, 360, 0));
            // JUIZO (5o pesado): salto alto e machadada que racha o chao
            case JUDGMENT -> keys(
                    k(0.20f, W).h(0.30f, -0.30f, -0.25f).a(10, 100, 0).b(10, 10).m(0, 0, 0.7f),
                    k(0.45f, W).h(0.32f, 0.46f, -0.05f).a(12, -70, 0).b(15, -20).r(0, 0, -15).m(0.4f, 0, 0),
                    k(0.66f, S).h(0.05f, -0.50f, -0.88f).a(-5, 150, 0).b(0, 38).r(0, 0, 15).m(0, 0.8f, 0.8f),
                    k(0.88f, F).h(0.05f, -0.64f, -0.76f).a(-5, 170, 0).b(0, 40).m(0, 0.8f, 0.85f),
                    rest(1));
            // QUEBRA-GUARDA (3o): golpe de cabo no queixo que levanta o mob
            case GUARD_BREAK -> keys(
                    k(0.30f, W).h(0.25f, -0.55f, -0.30f).a(10, 170, 0).b(20, 10).m(0, 0.2f, 0.5f),
                    k(0.45f, S).h(0.05f, 0.10f, -0.80f).a(-5, 10, 0).b(-5, -10).m(0.05f, 0.6f, 0.1f).d(0.15f),
                    k(0.65f, F).h(0.05f, 0.25f, -0.65f).a(-5, -10, 0).b(-5, -15).m(0, 0.6f, 0),
                    rest(1));
            // 4o do Quebra-Guarda: machadada no ar
            case BREAK_1 -> keys(
                    k(0.20f, W).h(0.40f, 0.35f, -0.25f).a(20, -40, 0).b(15, -12).m(0.2f, 0, 0),
                    k(0.34f, S).h(0.02f, -0.25f, -0.85f).a(-10, 120, 0).b(-5, 20).m(0.15f, 0, 0.1f),
                    k(0.52f, F).h(-0.15f, -0.50f, -0.65f).a(-20, 150, 0).b(-8, 22).m(0.1f, 0, 0.1f),
                    rest(1));
            // 5o do Quebra-Guarda: cambalhota no ar com machadada
            case BREAK_2 -> keys(
                    k(0.20f, W).h(0.62f, 0.0f, -0.30f).a(90, 70, -90).b(40, -6).m(0.25f, 0, 0),
                    k(0.40f, S).h(0.0f, -0.40f, -0.86f).a(-5, 140, -30).b(-10, 30).r(0, 0, 200).m(0.1f, 0.5f, 0.3f),
                    k(0.60f, F).h(0.0f, -0.60f, -0.70f).a(-5, 165, -30).b(0, 32).r(0, 0, 360).m(0, 0.5f, 0.5f),
                    rest(1, 0, 360));
            // saindo do rolamento: varrida baixa girando
            case ROLL_SLASH -> keys(
                    k(0.12f, W).h(0.60f, -0.60f, -0.35f).a(92, 110, -90).b(46, 12).m(0, 0, 0.8f),
                    k(0.32f, S).h(-0.05f, -0.40f, -0.85f).a(-15, 95, -90).b(-5, 10).r(180, 0, 0).m(0, 0.3f, 0.6f),
                    k(0.52f, F).h(-0.55f, -0.35f, -0.50f).a(-92, 95, -90).b(-46, 8).r(360, 0, 0).m(0, 0.3f, 0.4f),
                    rest(1, 360, 0));
            // saindo do rolamento pesado: machadada subindo do chao
            case ROLL_THRUST -> keys(
                    k(0.25f, W).h(0.50f, -0.60f, -0.30f).a(40, 160, 0).b(35, 12).m(0, 0, 0.6f),
                    k(0.40f, S).h(0.02f, -0.20f, -0.90f).a(-15, 100, 0).b(-10, 20).m(0.1f, 0.7f, 0.3f).d(0.3f),
                    k(0.62f, F).h(-0.25f, -0.55f, -0.60f).a(-30, 155, 0).b(-20, 24).m(0, 0.7f, 0.4f),
                    rest(1));
            // pesado: ergue atras da cabeca e racha
            case HEAVY -> keys(
                    k(0.38f, W).h(0.34f, 0.42f, -0.08f).a(12, -62, 0).b(16, -16).m(0, 0.2f, 0.25f),
                    k(0.45f, LIN).h(0.34f, 0.46f, -0.05f).a(12, -68, 0).b(16, -18).m(0, 0.2f, 0.25f),
                    k(0.50f, S).h(0.05f, -0.46f, -0.86f).a(-5, 142, 0).b(0, 34).m(0, 0.75f, 0.75f).d(0.15f),
                    k(0.72f, F).h(0.05f, -0.62f, -0.74f).a(-5, 166, 0).b(0, 36).m(0, 0.75f, 0.8f),
                    rest(1));
            // no ar: cambalhota e machadada
            case JUMP -> keys(
                    k(0.30f, W).h(0.20f, 0.46f, -0.14f).a(0, -72, 0).b(0, -22).r(0, 0, 170).m(0.3f, 0, 0),
                    k(0.55f, S).h(0.05f, -0.50f, -0.86f).a(-4, 150, 0).b(0, 38).r(0, 0, 360).m(0, 0.3f, 0.4f),
                    k(0.76f, F).h(0.05f, -0.62f, -0.74f).a(-4, 168, 0).b(0, 38).r(0, 0, 360).m(0, 0.3f, 0.5f),
                    rest(1, 0, 360));
            // investida: ombro na frente e golpe horizontal
            case FORWARD -> keys(
                    k(0.18f, W).h(0.64f, -0.20f, -0.30f).a(88, 85, -90).b(46, 10).m(0, 0.3f, 0.35f),
                    k(0.32f, S).h(-0.05f, -0.30f, -0.96f).a(-10, 88, -90).b(-10, 22).m(0, 0.9f, 0.4f).d(0.35f),
                    k(0.52f, F).h(-0.56f, -0.36f, -0.54f).a(-92, 90, -90).b(-46, 18).m(0, 0.9f, 0.3f),
                    rest(1));
            // parry: cabo atravessado
            case PARRY -> keys(
                    k(0.20f, S).h(0.05f, -0.10f, -0.50f).a(-80, 75, 90).b(-8, -6).m(0, 0.15f, 0.25f),
                    k(0.45f, F).h(0.20f, -0.05f, -0.55f).a(-50, 40, 90).b(8, -8).m(0, 0.15f, 0.2f),
                    rest(1));
            // Issen: machadada de cima atravessando
            case ISSEN -> keys(
                    k(0.10f, W).h(0.30f, 0.40f, -0.10f).a(10, -60, 0).b(15, -15).m(0, 0.2f, 0.4f),
                    k(0.25f, S).h(0.05f, -0.45f, -0.90f).a(-5, 145, 0).b(0, 35).m(0, 0.9f, 0.7f).d(0.6f),
                    k(0.55f, F).h(0.05f, -0.60f, -0.75f).a(-5, 165, 0).b(0, 36).m(0, 0.9f, 0.75f).d(0.5f),
                    rest(1));
            // Rajada: machadadas alternadas e uma final
            case RUSH -> keys(
                    k(0.08f, S).h(0.50f, 0.20f, -0.40f).a(40, -10, -20).b(25, -6).m(0, 0.3f, 0.2f),
                    k(0.20f, S).h(-0.10f, -0.40f, -0.85f).a(-20, 130, -20).b(-10, 15).m(0, 0.3f, 0.4f),
                    k(0.32f, S).h(-0.50f, -0.10f, -0.50f).a(-80, 80, 90).b(-30, 0).m(0, 0.3f, 0.3f),
                    k(0.44f, S).h(0.50f, -0.30f, -0.50f).a(90, 90, 90).b(30, 5).m(0, 0.3f, 0.3f),
                    k(0.58f, S).h(0.05f, -0.50f, -0.86f).a(-5, 150, 0).b(0, 30).m(0, 0.6f, 0.6f),
                    k(0.78f, F).h(0.05f, -0.55f, -0.82f).a(-5, 156, 0).b(0, 32).m(0, 0.6f, 0.65f),
                    rest(1));
            // golpe critico: execucao com machadada no chao
            case CRITICAL -> keys(
                    k(0.35f, W).h(0.34f, 0.46f, -0.05f).a(12, -70, 0).b(16, -18).m(0, 0.3f, 0.2f),
                    k(0.55f, S).h(0.05f, -0.60f, -0.86f).a(-5, 165, 0).b(0, 40).m(0, 1, 0.9f).d(0.2f),
                    k(0.82f, F).h(0.05f, -0.62f, -0.84f).a(-5, 168, 0).b(0, 40).m(0, 1, 0.9f).d(0.2f),
                    rest(1));
        };
    }

    private static float ease(int e, float a) {
        return switch (e) {
            case W, R -> a * a * (3 - 2 * a);
            case S -> a * a * a;                                   // acelera ate o impacto
            case F -> 1 - (1 - a) * (1 - a) * (1 - a);              // sai rapido e freia
            default -> a;
        };
    }

    // pose no tempo u (0..1)
    static float[] sample(Key[] k, float u) {
        float[] out = new float[N];
        if (u <= k[0].t()) { System.arraycopy(k[0].v(), 0, out, 0, N); return out; }
        for (int i = 0; i < k.length - 1; i++) {
            if (u <= k[i + 1].t()) {
                float a = (u - k[i].t()) / Math.max(1e-4f, k[i + 1].t() - k[i].t());
                a = ease(k[i + 1].ease(), a);
                for (int j = 0; j < N; j++) out[j] = k[i].v()[j] + (k[i + 1].v()[j] - k[i].v()[j]) * a;
                return out;
            }
        }
        System.arraycopy(k[k.length - 1].v(), 0, out, 0, N);
        return out;
    }

    // o trecho de golpe (para o rastro da lamina): do fim da preparacao ate o comeco da continuacao
    static boolean striking(Key[] k, float u) {
        for (int i = 1; i < k.length; i++) {
            if (k[i].ease() == S && u >= k[i - 1].t() - 0.02f && u <= k[i].t()) return true;
            if (k[i].ease() == F && u >= k[i - 1].t() && u <= k[i - 1].t() + (k[i].t() - k[i - 1].t()) * 0.6f) return true;
        }
        return false;
    }
}
