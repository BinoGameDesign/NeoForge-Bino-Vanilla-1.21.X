package com.bino.binosvanillaplus.client.bench;

import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Glass;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.UiSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tela "invisivel" da cena da bancada / bigorna: barra do inventario embaixo (vidro), numeros das pilhas no tampo,
// custo da bigorna em cima do resultado, campo de nome (bigorna) no alto e as dicas.
public class BenchSceneScreen extends Screen {
    private static final int SLOT = 20, GAP = 2, BOTTOM = 8;

    private boolean expanded;
    private final Tween open = new Tween(0f, 90f);
    private final Map<Integer, Tween> hoverAnim = new HashMap<>();
    private EditBox name;
    private ItemStack lastLeft = ItemStack.EMPTY;

    public BenchSceneScreen(Component title) { super(title); }

    @Override
    protected void init() {
        if (BenchScene.kind == BenchScene.Kind.ANVIL) {
            name = new EditBox(font, width / 2 - 80, 40, 160, 16, Component.translatable("container.repair"));
            name.setMaxLength(50);
            name.setBordered(false);
            name.setTextColor(0xFFFFFFFF);
            name.setResponder(this::onName);
            addWidget(name);
        }
    }

    private void onName(String s) {
        ItemStack left = BenchScene.slot(0);
        if (left.isEmpty()) return;
        if (!left.has(DataComponents.CUSTOM_NAME) && s.equals(left.getHoverName().getString())) s = "";
        BenchScene.rename(s);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private record Cell(int x, int y, int slot) {}

    private float slide() {
        return (float) LapidaryCut.inOutCubic(BenchScene.phase == BenchScene.Phase.ACTIVE ? 1 : BenchScene.weight);
    }

    private int x0() { return width / 2 - (9 * (SLOT + GAP) - GAP + SLOT + GAP) / 2; }
    private int y0() { return height - BOTTOM - SLOT + (int) ((1 - slide()) * 34); }

    private List<Cell> cells() {
        List<Cell> out = new ArrayList<>();
        int inv = BenchScene.invStart(), x0 = x0(), y0 = y0();
        for (int i = 0; i < 9; i++) out.add(new Cell(x0 + i * (SLOT + GAP), y0, inv + 27 + i));
        float o = open.get(expanded ? 1f : 0f);
        if (o > 0.02f)
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 9; c++)
                    out.add(new Cell(x0 + c * (SLOT + GAP), y0 - (int) ((3 - r) * (SLOT + GAP) * o) - (int) (4 * o), inv + r * 9 + c));
        return out;
    }

    private int[] plus() { return new int[]{x0() + 9 * (SLOT + GAP), y0()}; }

    private static boolean in(double mx, double my, int x, int y) { return mx >= x && my >= y && mx < x + SLOT && my < y + SLOT; }

    private boolean overUi(double mx, double my) {
        for (Cell c : cells()) if (in(mx, my, c.x, c.y)) return true;
        int[] p = plus();
        return in(mx, my, p[0], p[1]) || nameVisible() && mx >= width / 2 - 90 && mx < width / 2 + 90 && my >= 34 && my < 58;
    }

    private boolean nameVisible() { return name != null && !BenchScene.slot(0).isEmpty() && BenchScene.phase == BenchScene.Phase.ACTIVE; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Glass.capture();
        BenchScene.updateHover();
        boolean ui = overUi(mouseX, mouseY);
        if (ui) BenchScene.hover = -1;
        BenchScene.overBar = ui && BenchScene.phase == BenchScene.Phase.ACTIVE;
        ItemStack tip = ItemStack.EMPTY;
        boolean active = BenchScene.phase == BenchScene.Phase.ACTIVE;

        // numeros das pilhas no tampo e nome do item sob o mouse
        if (active) {
            for (int s = 0; s < BenchScene.slotCount(); s++) {
                if (!BenchScene.isInput(s)) continue;
                ItemStack st = BenchScene.slot(s);
                if (st.isEmpty()) continue;
                if (st.getCount() > 1) {
                    double[] p = LapidaryPick.project(BenchScene.slotPoint(s));
                    if (p != null) g.drawString(font, String.valueOf(st.getCount()), (int) p[0] + 4, (int) p[1], 0xFFFFFFFF, true);
                }
                if (s == BenchScene.hover) tip = st;
            }
            ItemStack r = BenchScene.slot(BenchScene.resultSlot());
            if (!r.isEmpty()) {
                double[] p = LapidaryPick.project(BenchScene.resultPoint());
                if (p != null && r.getCount() > 1) g.drawString(font, String.valueOf(r.getCount()), (int) p[0] + 8, (int) p[1] + 4, 0xFFFFFFFF, true);
                // custo da bigorna em cima do resultado
                int cost = BenchScene.cost();
                if (BenchScene.kind == BenchScene.Kind.ANVIL && cost > 0) {
                    double[] q = LapidaryPick.project(BenchScene.resultPoint().add(0, 0.2, 0));
                    if (q != null) {
                        boolean ok = BenchScene.canTake();
                        String t = cost >= 40 && !ok ? Component.translatable("container.repair.expensive").getString()
                                : Component.translatable("container.repair.cost", cost).getString();
                        g.drawString(font, t, (int) q[0] - font.width(t) / 2, (int) q[1] - 4, ok ? 0xFF80FF20 : 0xFFFF6060, true);
                    }
                }
                if (BenchScene.hover == BenchScene.resultSlot()) tip = r;
            }
        }

        // campo de nome (bigorna): aparece quando tem item na esquerda
        if (name != null) {
            ItemStack left = BenchScene.slot(0);
            if (!ItemStack.matches(left, lastLeft)) {
                lastLeft = left.copy();
                name.setValue(left.isEmpty() ? "" : left.getHoverName().getString());
            }
            if (nameVisible()) {
                Glass.panel(g, width / 2f - 90, 34, 180, 22, 7, Glass.POPUP.withRim(name.isFocused() ? 1.4f : 0.9f), BinoUi.MENU_BG);
                name.setX(width / 2 - 82);
                name.setY(41);
                name.render(g, mouseX, mouseY, partialTick);
            } else name.setFocused(false);
        }

        // barra do inventario
        List<Cell> cells = cells();
        int minY = Integer.MAX_VALUE;
        for (Cell c : cells) minY = Math.min(minY, c.y);
        int[] pl = plus();
        float bw = 10 * (SLOT + GAP) - GAP + 8, bh = y0() + SLOT - minY + 8;
        Glass.shadow(g, x0() - 4, minY - 4, bw, bh, 7, 3f, 6f, 0.4f);
        Glass.panel(g, x0() - 4, minY - 4, bw, bh, 7, Glass.PANEL, BinoUi.MENU_BG);
        for (Cell c : cells) {
            ItemStack s = BenchScene.slot(c.slot);
            boolean over = in(mouseX, mouseY, c.x, c.y);
            float h = hoverAnim.computeIfAbsent(c.slot, k -> new Tween(0f, 70f)).get(over ? 1f : 0f);
            Glass.panel(g, c.x, c.y, SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * h).withRim(0.55f + 0.7f * h), 0xFF2A2E2B);
            int lift = Math.round(h);
            g.renderItem(s, c.x + 2, c.y + 2 - lift);
            g.renderItemDecorations(font, s, c.x + 2, c.y + 2 - lift);
            if (over) tip = s;
        }
        boolean overPlus = in(mouseX, mouseY, pl[0], pl[1]);
        float hp = hoverAnim.computeIfAbsent(-1, k -> new Tween(0f, 70f)).get(overPlus ? 1f : 0f);
        Glass.panel(g, pl[0], pl[1], SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * hp).withRim(0.55f + 0.7f * hp), 0xFF2A2E2B);
        String t = expanded ? "-" : "+";
        BinoUi.text(g, font, t, pl[0] + SLOT / 2f - font.width(t), pl[1] + 3, 2f, BinoUi.TEXT, false);

        // dicas no alto
        float a = slide(), sc = 0.75f;
        String hint = BenchScene.hint();
        float ha = BenchScene.hintAlpha(hint);
        if (ha > 0.02f) {
            String s = Component.translatable(hint).getString();
            int col = alpha(hint.endsWith(".no") ? 0xFFFF7A6B : 0xFFFFFFFF, a * ha);
            BinoUi.text(g, font, s, width / 2f - font.width(s) * sc / 2f, 10, sc, col, true);
        }
        float la = BenchScene.leaveAlpha();
        if (la > 0.02f) {
            String s = Component.translatable("bench.binosvanillaplus.leave").getString();
            BinoUi.text(g, font, s, width / 2f - font.width(s) * sc / 2f, 21, sc, alpha(BinoUi.TEXT_DIM, a * 0.9f * la), true);
        }
        if (!tip.isEmpty() && BenchScene.carried().isEmpty()) g.renderTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (name != null && nameVisible() && mx >= width / 2 - 90 && mx < width / 2 + 90 && my >= 34 && my < 58) {
            name.setFocused(true);
            return name.mouseClicked(mx, my, button) || true;
        }
        if (name != null) name.setFocused(false);
        int[] pl = plus();
        if (in(mx, my, pl[0], pl[1])) { expanded = !expanded; UiSound.click(); return true; }
        for (Cell c : cells())
            if (in(mx, my, c.x, c.y)) { BenchScene.barClick(c.slot, button, hasShiftDown()); return true; }
        BenchScene.updateHover();
        if (my < plus()[1] - 12 && BenchScene.outside()) { BenchScene.leave(); return true; }
        BenchScene.click(button, hasShiftDown());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (keyCode == 256) { BenchScene.leave(); return true; }
        if (name != null && name.isFocused()) return name.keyPressed(keyCode, scanCode, modifiers) || true;
        if (mc.options.keyInventory.matches(keyCode, scanCode)) { BenchScene.leave(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (name != null && name.isFocused()) return name.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() { BenchScene.screenClosed(); }

    private static int alpha(int argb, float a) {
        int al = (int) (((argb >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }
}
