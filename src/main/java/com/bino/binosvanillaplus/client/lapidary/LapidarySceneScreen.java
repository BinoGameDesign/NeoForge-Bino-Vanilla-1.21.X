package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Glass;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.UiSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tela "invisivel" da cena da Mesa de Lapidacao: so a barra de itens no alto (silex e minerios brutos do inventario,
// silex primeiro; "+" mostra o resto) e a dica no rodape. Clique na barra = pega o item; clique no mundo = a cena
// decide (colocar na mesa, apertar o botao...). Esc ou a tecla de inventario saem da cena.
public class LapidarySceneScreen extends Screen {
    private static final int SLOT = 20, GAP = 2, PER_ROW = 9, TOP = 8;

    private boolean expanded;
    private final SceneExitButton exit = new SceneExitButton();
    private final Tween open = new Tween(0f, 90f);
    private final Map<Integer, Tween> hoverAnim = new HashMap<>();

    public LapidarySceneScreen() {
        super(Component.translatable("container.binosvanillaplus.lapidary_table"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}   // sem escurecer o jogo

    // ---------------------------------------------------------------- layout da barra
    private record Cell(int x, int y, int slot) {}

    private List<Integer> items() { return LapidaryScene.barSlots(); }

    private int barWidth(int shown, boolean plus) {
        return shown * (SLOT + GAP) - GAP + (plus ? SLOT + GAP : 0);
    }

    private float slide() {
        return (float) LapidaryCut.inOutCubic(LapidaryScene.phase == LapidaryScene.Phase.ACTIVE ? 1 : LapidaryScene.weight);
    }

    private java.util.ArrayList<Cell> cells() {
        List<Integer> list = items();
        int shown = Math.min(PER_ROW, list.size());
        boolean plus = list.size() > PER_ROW;
        int x0 = width / 2 - barWidth(Math.max(1, shown), plus) / 2;
        int y0 = TOP + (int) ((1 - slide()) * -34);
        java.util.ArrayList<Cell> out = new java.util.ArrayList<>();
        for (int i = 0; i < shown; i++) out.add(new Cell(x0 + i * (SLOT + GAP), y0, list.get(i)));
        float o = open.get(expanded ? 1f : 0f);
        if (plus && o > 0.02f) {
            for (int i = PER_ROW; i < list.size(); i++) {
                int r = (i - PER_ROW) / PER_ROW + 1, c = (i - PER_ROW) % PER_ROW;
                int y = y0 + (int) (r * (SLOT + GAP) * o);
                out.add(new Cell(x0 + c * (SLOT + GAP), y, list.get(i)));
            }
        }
        return out;
    }

    private int[] plusButton() {
        List<Integer> list = items();
        if (list.size() <= PER_ROW) return null;
        int x0 = width / 2 - barWidth(PER_ROW, true) / 2;
        int y0 = TOP + (int) ((1 - slide()) * -34);
        return new int[]{x0 + PER_ROW * (SLOT + GAP), y0};
    }

    private static boolean in(double mx, double my, int x, int y) {
        return mx >= x && my >= y && mx < x + SLOT && my < y + SLOT;
    }

    // ---------------------------------------------------------------- desenho
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Glass.capture();   // vidro dos menus: copia e desfoca a cena atras da barra
        LapidaryScene.updateHover();
        boolean overUi = overUi(mouseX, mouseY) || exit.hit(font, width, mouseX, mouseY);
        if (overUi) LapidaryScene.hover = LapidaryScene.Target.NONE;
        LapidaryScene.overBar = overUi || mouseY < TOP + SLOT + 10 && LapidaryScene.phase == LapidaryScene.Phase.ACTIVE;

        List<Integer> list = items();
        var cells = cells();
        int[] plus = plusButton();
        boolean busy = LapidaryScene.cutting();

        // fundo da barra
        if (!cells.isEmpty() || list.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (Cell c : cells) { minX = Math.min(minX, c.x); minY = Math.min(minY, c.y); maxX = Math.max(maxX, c.x + SLOT); maxY = Math.max(maxY, c.y + SLOT); }
            if (plus != null) { maxX = Math.max(maxX, plus[0] + SLOT); }
            if (list.isEmpty()) {
                String t = Component.translatable("lapidary.binosvanillaplus.nothing").getString();
                int w = font.width(t) + 16, y0 = TOP + (int) ((1 - slide()) * -34);
                Glass.shadow(g, width / 2f - w / 2f, y0, w, SLOT, 7, 3f, 6f, 0.4f);
                Glass.panel(g, width / 2f - w / 2f, y0, w, SLOT, 7, Glass.PANEL, BinoUi.MENU_BG);
                g.drawString(font, t, width / 2 - font.width(t) / 2, y0 + 6, BinoUi.TEXT_DIM, false);
            } else {
                Glass.shadow(g, minX - 4, minY - 4, maxX - minX + 8, maxY - minY + 8, 7, 3f, 6f, 0.4f);
                Glass.panel(g, minX - 4, minY - 4, maxX - minX + 8, maxY - minY + 8, 7, Glass.PANEL, BinoUi.MENU_BG);
            }
        }

        // itens
        ItemStack tip = ItemStack.EMPTY;
        LapidaryMenuView menu = LapidaryMenuView.of();
        for (Cell c : cells) {
            ItemStack s = menu.item(c.slot);
            boolean over = !busy && in(mouseX, mouseY, c.x, c.y);
            float h = hoverAnim.computeIfAbsent(c.slot, k -> new Tween(0f, 70f)).get(over ? 1f : 0f);
            int bg = mix(0xFF2A2E2B, 0xFF484A49, h);
            Glass.panel(g, c.x, c.y, SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * h).withRim(0.55f + 0.7f * h), bg);
            int lift = Math.round(h);
            g.renderItem(s, c.x + 2, c.y + 2 - lift);
            g.renderItemDecorations(font, s, c.x + 2, c.y + 2 - lift);
            if (over) tip = s;
        }
        if (plus != null) {
            boolean over = in(mouseX, mouseY, plus[0], plus[1]);
            float h = hoverAnim.computeIfAbsent(-1, k -> new Tween(0f, 70f)).get(over ? 1f : 0f);
            Glass.panel(g, plus[0], plus[1], SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * h).withRim(0.55f + 0.7f * h), mix(0xFF2A2E2B, 0xFF484A49, h));
            String t = expanded ? "-" : "+";
            BinoUi.text(g, font, t, plus[0] + SLOT / 2f - font.width(t), plus[1] + 3, 2f, BinoUi.TEXT, false);
            if (over) g.renderTooltip(font, Component.translatable("lapidary.binosvanillaplus.more"), mouseX, mouseY);
        }

        // dica no rodape
        float a = slide();
        float sc = 0.75f;   // legenda menor
        String hint = LapidaryScene.hint();
        float ha = LapidaryScene.hintAlpha(hint);
        if (!hint.isEmpty() && ha > 0.02f) {
            String t = Component.translatable(hint).getString();
            boolean wrong = hint.endsWith(".wrong");
            int col = alpha(wrong ? 0xFFFF7A6B : 0xFFFFFFFF, a * ha);
            BinoUi.text(g, font, t, width / 2f - font.width(t) * sc / 2f, height - 34, sc, col, true);
        }
        float la = LapidaryScene.leaveAlpha();
        if (la > 0.02f) {
            String leave = Component.translatable("lapidary.binosvanillaplus.hint.leave").getString();
            BinoUi.text(g, font, leave, width / 2f - font.width(leave) * sc / 2f, height - 22, sc, alpha(BinoUi.TEXT_DIM, a * 0.9f * la), true);
        }

        exit.render(g, font, width, mouseX, mouseY, slide());
        if (!tip.isEmpty()) g.renderTooltip(font, tip, mouseX, mouseY);
    }

    private boolean overUi(double mx, double my) {
        for (Cell c : cells()) if (in(mx, my, c.x, c.y)) return true;
        int[] plus = plusButton();
        return plus != null && in(mx, my, plus[0], plus[1]);
    }

    // ---------------------------------------------------------------- entrada
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (exit.hit(font, width, mx, my)) { LapidaryScene.leave(); return true; }
        int[] plus = plusButton();
        if (plus != null && in(mx, my, plus[0], plus[1])) {
            expanded = !expanded;
            UiSound.click();
            return true;
        }
        for (Cell c : cells()) {
            if (in(mx, my, c.x, c.y)) {
                LapidaryScene.barClick(c.slot, button);
                return true;
            }
        }
        LapidaryScene.updateHover();
        LapidaryScene.click(button, hasShiftDown());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (keyCode == 256 || mc.options.keyInventory.matches(keyCode, scanCode)) {
            LapidaryScene.leave();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }   // o Esc e' tratado acima (sai com animacao)

    @Override
    public void removed() {
        LapidaryScene.screenClosed();
    }

    // ---------------------------------------------------------------- cores
    private static int alpha(int argb, float a) {
        int al = (int) (((argb >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        int a = (int) Math.round(((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) Math.round(((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int gg = (int) Math.round(((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) Math.round((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    // leitura dos itens do menu aberto
    private record LapidaryMenuView(com.bino.binosvanillaplus.menu.LapidaryMenu menu) {
        static LapidaryMenuView of() { return new LapidaryMenuView(LapidaryScene.menu()); }
        ItemStack item(int slot) {
            return menu == null || slot < 0 || slot >= menu.slots.size() ? ItemStack.EMPTY : menu.getSlot(slot).getItem();
        }
    }
}
