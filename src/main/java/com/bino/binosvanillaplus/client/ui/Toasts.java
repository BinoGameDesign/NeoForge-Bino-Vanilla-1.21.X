package com.bino.binosvanillaplus.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Avisos curtos no rodape das telas ("Coordenadas copiadas", "Marcação excluída — Desfazer").
// Sobem com fade, ficam alguns segundos e somem. Podem ter uma acao clicavel (ex.: desfazer).
// Desenhados e clicados pelo BinoScreen; chame Toasts.show(...) de qualquer lugar.
public final class Toasts {
    private Toasts() {}

    private static final class Toast {
        final String text;
        @Nullable final String action;
        @Nullable final Runnable run;
        final long at;
        final int ms;
        long dismissAt = -1;
        int bx, by, bw, bh, ax0, ax1;   // area da acao (preenchida ao desenhar)
        Toast(String text, @Nullable String action, @Nullable Runnable run, int ms) {
            this.text = text; this.action = action; this.run = run; this.at = Ease.now(); this.ms = ms;
        }
    }

    private static final List<Toast> LIST = new ArrayList<>();
    private static final int IN = 160, OUT = 220;

    public static void show(String text) { add(new Toast(text, null, null, 2200)); }

    public static void show(String text, String action, Runnable run) { add(new Toast(text, action, run, 4500)); }

    private static void add(Toast t) {
        LIST.add(t);
        while (LIST.size() > 3) LIST.remove(0);
    }

    public static void render(GuiGraphics g, int screenW, int screenH, int mx, int my) {
        if (LIST.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        long now = Ease.now();
        LIST.removeIf(t -> (t.dismissAt >= 0 ? now - t.dismissAt > OUT : now - t.at > t.ms + OUT));
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        float stackY = screenH - 10;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Toast t = LIST.get(i);
            float in = Ease.outCubic((now - t.at) / (float) IN);
            long outStart = t.dismissAt >= 0 ? t.dismissAt : t.at + t.ms;
            float out = Ease.inCubic((now - outStart) / (float) OUT);
            float vis = Math.min(in, 1f - out);
            if (vis <= 0.01f) continue;
            String act = t.action == null ? "" : "   " + t.action;
            int w = font.width(t.text) + font.width(act) + 16, h = 16;
            int x = (screenW - w) / 2;
            float y = stackY - h + (1f - in) * 10f + out * 6f;
            t.bx = x; t.by = (int) y; t.bw = w; t.bh = h;
            BinoUi.round(g, x, (int) y, w, h, Ease.alpha(0xF0151716, vis));
            g.fill(x + 2, (int) y + h - 2, x + 2 + (int) ((w - 4) * (1f - Ease.clamp((now - t.at) / (float) t.ms))), (int) y + h - 1,
                    Ease.alpha(0x60FFFFFF, vis));   // barrinha de tempo restante
            int tc = Ease.alpha(0xFFFFFFFF, vis);
            g.drawString(font, t.text, x + 8, (int) y + 4, tc, false);
            if (t.action != null) {
                int ax = x + 8 + font.width(t.text) + font.width("   ");
                t.ax0 = ax; t.ax1 = ax + font.width(t.action);
                boolean over = mx >= t.ax0 && mx < t.ax1 && my >= y && my < y + h;
                int ac = Ease.alpha(over ? 0xFFD6D6D6 : BinoUi.ACCENT, vis);
                g.drawString(font, t.action, ax, (int) y + 4, ac, false);
                g.fill(ax, (int) y + 13, t.ax1, (int) y + 14, ac);
            }
            stackY -= (h + 4) * vis;
        }
        g.pose().popPose();
    }

    // true se o clique foi numa acao de aviso
    public static boolean click(double mx, double my) {
        for (Toast t : LIST) {
            if (t.action == null || t.run == null || t.dismissAt >= 0) continue;
            if (mx >= t.ax0 - 2 && mx < t.ax1 + 2 && my >= t.by && my < t.by + t.bh) {
                t.dismissAt = Ease.now();
                UiSound.click();
                t.run.run();
                return true;
            }
        }
        return false;
    }
}
