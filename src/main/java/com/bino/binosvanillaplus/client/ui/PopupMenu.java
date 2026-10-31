package com.bino.binosvanillaplus.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Menu flutuante animado: serve de menu de contexto (botao direito) e de lista suspensa (dropdown).
//  - abre "crescendo" a partir do ponto de origem (escala + fade), fecha com fade
//  - destaque desliza suavemente entre os itens; item marcado aparece em amarelo
//  - teclado: setas, Enter, Esc
// Uso: menu.open(x, y, itens) ; no render: menu.render(g, mx, my) ; nos eventos: menu.mouseClicked / keyPressed
public class PopupMenu {
    public record Item(String label, @Nullable Runnable action, boolean checked) {
        public static Item of(String label, Runnable action) { return new Item(label, action, false); }
    }

    private final List<Item> items = new ArrayList<>();
    @Nullable private final String title;      // "Menu" em fonte 2x no topo (null = sem titulo, estilo dropdown)
    private int x, y, w;
    private float originX, originY;
    private long openedAt = -1, closedAt = -1;
    private int keyIndex = -1;
    private final Tween highlight = new Tween(0f, 35f), highlightA = new Tween(0f, 50f);
    private static final int ROW = 13, PAD_X = 7;
    private static final int OPEN_MS = 140, CLOSE_MS = 90;

    public PopupMenu(@Nullable String title) { this.title = title; }

    private int head() { return title == null ? 2 : 14; }

    public int height() { return head() + items.size() * ROW + (title == null ? 2 : 5); }

    public int width() { return w; }

    public boolean isOpen() { return openedAt >= 0 && closedAt < 0; }

    // ainda desenhando (abrindo, aberto ou terminando de fechar)
    public boolean isVisible() { return openedAt >= 0 && (closedAt < 0 || Ease.since(closedAt, CLOSE_MS) < 1f); }

    // abre em (ax, ay), mantendo-se dentro de (0,0)-(screenW,screenH); minWidth para alinhar com um botao
    public void open(int ax, int ay, List<Item> list, int minWidth, int screenW, int screenH) {
        items.clear();
        items.addAll(list);
        if (items.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int widest = title == null ? 0 : font.width(title) * 2 + 8;
        for (Item it : items) widest = Math.max(widest, font.width(it.label()));
        w = Math.max(minWidth, widest + PAD_X * 2 + 2);
        x = Math.max(2, Math.min(ax, screenW - w - 2));
        y = Math.max(title == null ? 2 : 10, Math.min(ay, screenH - height() - 2));
        originX = ax; originY = ay;
        openedAt = Ease.now();
        closedAt = -1;
        keyIndex = -1;
        highlightA.snap(0f);
        UiSound.tick();
    }

    public void close() {
        if (isOpen()) closedAt = Ease.now();
    }

    private int rowY(int i) { return y + head() + i * ROW; }

    private int indexAt(double mx, double my) {
        if (mx < x || mx >= x + w) return -1;
        for (int i = 0; i < items.size(); i++) if (my >= rowY(i) && my < rowY(i) + ROW) return i;
        return -1;
    }

    public void render(GuiGraphics g, int mx, int my) {
        if (!isVisible()) return;
        Font font = Minecraft.getInstance().font;
        float vis = closedAt >= 0 ? 1f - Ease.inCubic(Ease.since(closedAt, CLOSE_MS)) : Ease.outCubic(Ease.since(openedAt, OPEN_MS));
        float s = closedAt >= 0 ? 1f : 0.85f + 0.15f * Ease.outBack(Ease.since(openedAt, OPEN_MS));

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.pose().translate(originX, originY, 0);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-originX, -originY, 0);
        g.flush();
        float[] prev = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3] * vis);

        int h = height();
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x40000000);   // sombra
        BinoUi.round(g, x, y, w, h, title == null ? 0xF02A2D2B : BinoUi.MENU_BG);
        if (title != null) BinoUi.titleLeft(g, font, title, x + 10, y - 7);

        int hov = closedAt < 0 ? indexAt(mx, my) : -1;
        if (hov >= 0) keyIndex = -1;
        int target = hov >= 0 ? hov : keyIndex;
        if (target >= 0) {
            highlight.to(rowY(target));
            if (highlightA.target() < 0.5f) highlight.snap(rowY(target));   // primeira vez: aparece direto no item
        }
        float ha = highlightA.get(target >= 0 ? 1f : 0f);
        if (ha > 0.01f) BinoUi.round(g, x + 2, (int) highlight.get(), w - 4, ROW, Ease.alpha(0x38FFFFFF, ha));

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            int col = it.action() == null ? 0xFF7A7A7A : it.checked() ? BinoUi.ACCENT : 0xFFBDBDBD;
            g.drawString(font, it.label(), x + PAD_X + (i == target ? 1 : 0), rowY(i) + 3, col, false);
        }
        g.flush();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
        g.pose().popPose();
    }

    // true = o menu tratou o clique (inclusive clique fora, que so fecha)
    public boolean mouseClicked(double mx, double my) {
        if (!isOpen()) return false;
        int i = indexAt(mx, my);
        if (i >= 0) {
            run(i);
            return true;
        }
        close();   // clique fora so fecha
        return true;
    }

    private void run(int i) {
        Item it = items.get(i);
        if (it.action() == null) return;
        UiSound.click();
        close();
        it.action().run();
    }

    // setas / Enter / Esc
    public boolean keyPressed(int key) {
        if (!isOpen()) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            int n = items.size(), d = key == GLFW.GLFW_KEY_DOWN ? 1 : -1;
            int k = keyIndex < 0 ? (d > 0 ? -1 : n) : keyIndex;
            for (int tries = 0; tries < n; tries++) {
                k = (k + d + n) % n;
                if (items.get(k).action() != null) break;
            }
            keyIndex = k;
            UiSound.tick();
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) && keyIndex >= 0) {
            run(keyIndex);
            return true;
        }
        return false;
    }

    public boolean contains(double mx, double my) { return isOpen() && mx >= x && mx < x + w && my >= y && my < y + height(); }
}
