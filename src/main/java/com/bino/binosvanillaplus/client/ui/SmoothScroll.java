package com.bino.binosvanillaplus.client.ui;

import net.minecraft.client.gui.GuiGraphics;

// Rolagem suave em pixels + barra de rolagem fina que aparece ao passar o mouse/rolar e pode ser arrastada.
// Uso: scroll.setBounds(area) ; offset = scroll.offset() ; scroll.renderBar(g, mx, my) ; repasse a roda e o arraste.
public class SmoothScroll {
    private final Tween pos = new Tween(0f, 55f), barA = new Tween(0f, 80f);
    private float target = 0f;
    private int x, y, w, h, content;
    private long lastActivity = -10000;
    private boolean dragging;
    private double dragStartY;
    private float dragStartTarget;

    public void setBounds(int x, int y, int w, int h, int contentHeight) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.content = contentHeight;
        target = clamp(target);
    }

    private float max() { return Math.max(0, content - h); }

    private float clamp(float t) { return Math.max(0, Math.min(max(), t)); }

    public float offset() { return pos.get(clamp(target)); }

    public void scrollBy(float px) { target = clamp(target + px); lastActivity = Ease.now(); }

    public void reset() { target = 0; pos.snap(0); }

    private int barX() { return x + w - 4; }

    private int thumbH() { return Math.max(14, (int) (h * (h / (float) Math.max(h, content)))); }

    private int thumbY() { return y + (int) ((h - thumbH()) * (max() <= 0 ? 0 : offset() / max())); }

    public void renderBar(GuiGraphics g, int mx, int my) {
        if (max() <= 0) return;
        boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
        boolean recent = Ease.now() - lastActivity < 900;
        float a = barA.get(dragging ? 1f : over || recent ? 0.75f : 0.25f);
        g.fill(barX(), y + 2, barX() + 2, y + h - 2, Ease.alpha(0x30FFFFFF, a));
        BinoUi.round(g, barX() - (dragging ? 1 : 0), thumbY(), dragging ? 4 : 2, thumbH(), Ease.alpha(0xFFD9D9D9, a));
    }

    public boolean mouseClicked(double mx, double my) {
        if (max() <= 0 || mx < barX() - 4 || mx > barX() + 4 || my < y || my >= y + h) return false;
        dragging = true;
        dragStartY = my;
        dragStartTarget = target;
        if (my < thumbY() || my > thumbY() + thumbH()) {   // clique na trilha: pula para la
            target = clamp((float) ((my - y - thumbH() / 2.0) / (h - thumbH()) * max()));
            dragStartTarget = target;
        }
        return true;
    }

    public boolean mouseDragged(double my) {
        if (!dragging) return false;
        float per = max() / Math.max(1, h - thumbH());
        target = clamp(dragStartTarget + (float) ((my - dragStartY) * per));
        lastActivity = Ease.now();
        return true;
    }

    public void mouseReleased() { dragging = false; }
}
