package com.bino.binosmining.client;

import com.bino.binosmining.client.lapidary.LapidaryCut;

// Olhar em volta nas cenas (Mesa de Lapidacao, bancada, bigorna, bau): com o mouse encostado na borda de cima da tela
// a camera olha para cima; na borda da esquerda ou da direita, vira para o lado. Saiu da borda, volta sozinha.
public final class SceneLook {
    public static final double EDGE = 0.9;          // quanto da tela conta como "borda" (0,9 = os ultimos 5%)
    public static final float UP_DEG = 32f, SIDE_DEG = 45f;

    private double up, side;                       // 0..1 e -1..1, suavizados

    public void update(double[] ndc, boolean active, double dt) {
        double k = 1 - Math.exp(-3.5 * dt);
        up += ((active && ndc[1] > EDGE ? 1 : 0) - up) * k;
        double target = !active ? 0 : ndc[0] > EDGE ? 1 : ndc[0] < -EDGE ? -1 : 0;
        side += (target - side) * k;
    }

    public float pitch() { return -UP_DEG * (float) LapidaryCut.inOutCubic(up); }

    // + = direita
    public float yaw() { return Math.signum((float) side) * SIDE_DEG * (float) LapidaryCut.inOutCubic(Math.abs(side)); }

    // olhando em volta (ou com o mouse na borda): clicar nao conta como "sair da cena"
    public boolean busy(double[] ndc) {
        return up > 0.05 || Math.abs(side) > 0.05 || ndc[1] > EDGE || Math.abs(ndc[0]) > EDGE;
    }

    public void reset() { up = 0; side = 0; }
}
