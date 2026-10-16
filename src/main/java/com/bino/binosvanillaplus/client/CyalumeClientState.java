package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.network.CyalumeStatePayload;

// Guarda o que a tela precisa mostrar. Não usa nenhuma classe exclusiva do cliente.
public class CyalumeClientState {

    public enum Phase { NONE, FLYING, LANDED }

    public static Phase phase = Phase.NONE;
    public static long startNanos;     // instante em que começou a cair
    public static long landNanos;      // instante em que pousou
    public static float height;        // altura da queda em blocos

    public static void handle(CyalumeStatePayload payload) {
        switch (payload.action()) {
            case CyalumeStatePayload.START -> {
                phase = Phase.FLYING;
                startNanos = System.nanoTime();
            }
            case CyalumeStatePayload.LAND -> {
                phase = Phase.LANDED;
                landNanos = System.nanoTime();
                height = payload.height();
            }
            default -> phase = Phase.NONE;   // CANCEL
        }
    }

    // Limpa a tela sozinha: 12 s depois de pousar, ou 30 s se alguma mensagem se perder
    public static void expireIfNeeded() {
        long now = System.nanoTime();
        if (phase == Phase.LANDED && now - landNanos > 4_000_000_000L) phase = Phase.NONE;
        if (phase == Phase.FLYING && now - startNanos > 30_000_000_000L) phase = Phase.NONE;
    }
}