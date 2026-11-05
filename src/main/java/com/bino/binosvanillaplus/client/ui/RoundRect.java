package com.bino.binosvanillaplus.client.ui;

import net.minecraft.client.gui.GuiGraphics;

// Retangulo de cantos arredondados (faixas horizontais), em coordenadas de GUI com fracao
public final class RoundRect {
    private RoundRect() {}

    public static void fill(GuiGraphics g, float x, float y, float w, float h, float r, int c) {
        int x0 = Math.round(x), y0 = Math.round(y), x1 = Math.round(x + w), y1 = Math.round(y + h);
        int rr = Math.max(0, Math.min(Math.round(r), Math.min(x1 - x0, y1 - y0) / 2));
        if (rr <= 0) { g.fill(x0, y0, x1, y1, c); return; }
        g.fill(x0, y0 + rr, x1, y1 - rr, c);
        for (int i = 0; i < rr; i++) {
            double dy = rr - i - 0.5;
            int inset = (int) Math.round(rr - Math.sqrt(rr * rr - dy * dy));
            g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, c);
            g.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, c);
        }
    }
}
