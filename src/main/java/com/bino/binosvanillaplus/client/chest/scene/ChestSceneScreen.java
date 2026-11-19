package com.bino.binosvanillaplus.client.chest.scene;

import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Glass;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.UiSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tela "invisivel" da cena do bau: barra do inventario embaixo (barra de atalhos; "+" abre as outras 3 fileiras),
// numero de cada pilha em cima do item dentro do bau, nome do item sob o mouse e a dica.
public class ChestSceneScreen extends Screen {
    private static final int SLOT = 20, GAP = 2, BOTTOM = 8;

    private boolean expanded;
    private final Tween open = new Tween(0f, 90f);
    private final Map<Integer, Tween> hoverAnim = new HashMap<>();

    public ChestSceneScreen(Component title) { super(title); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private record Cell(int x, int y, int slot) {}

    private float slide() {
        return (float) LapidaryCut.inOutCubic(ChestScene.phase == ChestScene.Phase.ACTIVE ? 1 : ChestScene.weight);
    }

    private int x0() { return width / 2 - (9 * (SLOT + GAP) - GAP + SLOT + GAP) / 2; }
    private int y0() { return height - BOTTOM - SLOT + (int) ((1 - slide()) * 34); }

    // slots do menu: depois das casas do bau vem o inventario (27) e a barra de atalhos (9)
    private List<Cell> cells() {
        List<Cell> out = new ArrayList<>();
        int inv = ChestScene.chestSlots();
        int x0 = x0(), y0 = y0();
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
        return in(mx, my, p[0], p[1]);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Glass.capture();   // vidro dos menus: copia e desfoca a cena atras da barra
        ChestScene.updateHover();
        boolean ui = overUi(mouseX, mouseY);
        if (ui) ChestScene.hoverSlot = -1;
        ChestScene.overBar = ui && ChestScene.phase == ChestScene.Phase.ACTIVE;
        ItemStack tip = ItemStack.EMPTY;

        // numeros das pilhas dentro do bau
        if (ChestScene.phase == ChestScene.Phase.ACTIVE) {
            for (int i = 0; i < ChestScene.chestSlots(); i++) {
                ItemStack s = ChestScene.slot(i);
                if (s.isEmpty()) continue;
                double[] p = LapidaryPick.project(ChestScene.cellPoint(i).add(0, 0.02, 0.0));
                if (p == null) continue;
                if (s.getCount() > 1) {
                    String n = String.valueOf(s.getCount());
                    BinoUi.text(g, font, n, (float) p[0] + 3, (float) p[1] + 1, 0.75f, 0xFFFFFFFF, true);
                }
                if (i == ChestScene.hoverSlot) tip = s;
            }
        }

        // barra do inventario
        List<Cell> cells = cells();
        int minY = Integer.MAX_VALUE;
        for (Cell c : cells) minY = Math.min(minY, c.y);
        int[] pl = plus();
        float bw = 10 * (SLOT + GAP) - GAP + 8, bh = y0() + SLOT - minY + 8;
        Glass.shadow(g, x0() - 4, minY - 4, bw, bh, 7, 3f, 6f, 0.4f);
        Glass.panel(g, x0() - 4, minY - 4, bw, bh, 7, Glass.PANEL, BinoUi.MENU_BG);
        ChestMenu menu = ChestScene.menu();
        for (Cell c : cells) {
            ItemStack s = menu == null ? ItemStack.EMPTY : menu.getSlot(c.slot).getItem();
            boolean over = in(mouseX, mouseY, c.x, c.y);
            float h = hoverAnim.computeIfAbsent(c.slot, k -> new Tween(0f, 70f)).get(over ? 1f : 0f);
            Glass.panel(g, c.x, c.y, SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * h).withRim(0.55f + 0.7f * h), mix(0xFF2A2E2B, 0xFF484A49, h));
            int lift = Math.round(h);
            g.renderItem(s, c.x + 2, c.y + 2 - lift);
            g.renderItemDecorations(font, s, c.x + 2, c.y + 2 - lift);
            if (over) tip = s;
        }
        boolean overPlus = in(mouseX, mouseY, pl[0], pl[1]);
        float hp = hoverAnim.computeIfAbsent(-1, k -> new Tween(0f, 70f)).get(overPlus ? 1f : 0f);
        Glass.panel(g, pl[0], pl[1], SLOT, SLOT, 4, Glass.SLOT.withMilk(0.08f * hp).withRim(0.55f + 0.7f * hp), mix(0xFF2A2E2B, 0xFF484A49, hp));
        String t = expanded ? "-" : "+";
        BinoUi.text(g, font, t, pl[0] + SLOT / 2f - font.width(t), pl[1] + 3, 2f, BinoUi.TEXT, false);

        // espacos do bau "usados/total", em cima dos itens (no fundo do bau, perto da tampa)
        double[] cp = menu == null || ChestScene.phase != ChestScene.Phase.ACTIVE ? null
                : LapidaryPick.project(ChestScene.local(0, 0.66, -ChestScene.HALF_DEPTH - 0.06));
        if (cp != null) {
            int used = 0, total = ChestScene.chestSlots();
            for (int i = 0; i < total; i++) if (!menu.getSlot(i).getItem().isEmpty()) used++;
            String t2 = used + "/" + total;
            int col = used >= total ? 0xFFFF7A6B : used >= total * 0.8 ? 0xFFFFD36B : 0xFFFFFFFF;
            g.drawString(font, t2, (int) cp[0] - font.width(t2) / 2, (int) cp[1] - 4, col, true);   // so o numero
        }

        // dicas (no alto, a barra fica embaixo)
        float a = slide(), sc = 0.75f;
        String hint = ChestScene.hint();
        float ha = ChestScene.hintAlpha(hint);
        if (ha > 0.02f) {
            String s = Component.translatable(hint).getString();
            int col = alpha(hint.endsWith(".full") ? 0xFFFF7A6B : 0xFFFFFFFF, a * ha);
            BinoUi.text(g, font, s, width / 2f - font.width(s) * sc / 2f, 14, sc, col, true);
        }
        float la = ChestScene.leaveAlpha();
        if (la > 0.02f) {
            String s = Component.translatable("chest.binosvanillaplus.hint.leave").getString();
            BinoUi.text(g, font, s, width / 2f - font.width(s) * sc / 2f, 26, sc, alpha(BinoUi.TEXT_DIM, a * 0.9f * la), true);
        }

        if (!tip.isEmpty() && ChestScene.carried().isEmpty()) g.renderTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] pl = plus();
        if (in(mx, my, pl[0], pl[1])) { expanded = !expanded; UiSound.click(); return true; }
        for (Cell c : cells())
            if (in(mx, my, c.x, c.y)) { ChestScene.barClick(c.slot, button, hasShiftDown()); return true; }
        ChestScene.updateHover();
        // clicar fora do bau sai da cena (perto da barra nao conta, para nao sair sem querer)
        if (my < plus()[1] - 12 && ChestScene.outside()) { ChestScene.leave(); return true; }
        ChestScene.click(button, hasShiftDown());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (keyCode == 256 || mc.options.keyInventory.matches(keyCode, scanCode)) { ChestScene.leave(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() { ChestScene.screenClosed(); }

    private static int alpha(int argb, float a) {
        int al = (int) (((argb >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        int a = Math.round(((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = Math.round(((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int gg = Math.round(((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = Math.round((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }
}
