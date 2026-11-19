package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Tween;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// Botao "Sair" no canto de cima, a direita, das cenas estilo cinema (Mesa de Lapidacao e bau).
public final class SceneExitButton {
    private static final int H = 18, MARGIN = 8;
    private final Tween hover = new Tween(0f, 70f);

    private static String label() { return "✕  " + Component.translatable("scene.binosvanillaplus.leave").getString(); }

    private static int w(Font font) { return font.width(label()) + 16; }

    public boolean hit(Font font, int screenW, double mx, double my) {
        int x = screenW - MARGIN - w(font);
        return mx >= x && mx < x + w(font) && my >= MARGIN && my < MARGIN + H;
    }

    // alpha: 0..1 (some junto com a cena)
    public void render(GuiGraphics g, Font font, int screenW, int mx, int my, float alpha) {
        if (alpha <= 0.02f) return;
        int w = w(font), x = screenW - MARGIN - w;
        float h = hover.get(hit(font, screenW, mx, my) ? 1f : 0f);
        int bg = mix(0xD81E211F, 0xF0484A49, h);
        BinoUi.round(g, x, MARGIN - (int) ((1 - alpha) * 30), w, H, bg);
        if (h > 0.01f) BinoUi.outline(g, x, MARGIN - (int) ((1 - alpha) * 30), w, H, (((int) (h * 200)) << 24) | 0xFFFFFF);
        g.drawString(font, label(), x + 8, MARGIN + 5 - (int) ((1 - alpha) * 30), 0xFFFFFFFF, false);
    }

    private static int mix(int c1, int c2, float t) {
        int a = Math.round(((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = Math.round(((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int gg = Math.round(((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = Math.round((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }
}
