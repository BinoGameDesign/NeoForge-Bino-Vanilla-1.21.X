package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.client.ui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

// Escolha do icone de uma marcacao: todos os blocos do jogo (como nas guias do criativo), com busca.
public class BlockPickerScreen extends BinoScreen {
    private static final int PAD = 14, CELL = 20;
    private static final Set<String> HIDDEN = Set.of("barrier", "light", "structure_void", "structure_block", "jigsaw",
            "command_block", "chain_command_block", "repeating_command_block", "grave", "test_block", "test_instance_block");

    private record Entry(ItemStack stack, String id, String name) {}

    private static List<Entry> ALL;

    private final Consumer<String> onPick;
    private final String current;
    private final int tint;
    private EditBox search;
    private int L, T, W, H, cols, rows, scroll;
    private final List<Entry> shown = new ArrayList<>();

    public BlockPickerScreen(@Nullable Screen parent, String currentSymbol, int tint, Consumer<String> onPick) {
        super("Escolher bloco", parent);
        this.current = currentSymbol == null ? "" : currentSymbol;
        this.tint = tint;
        this.onPick = onPick;
    }

    private static List<Entry> all() {
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof BlockItem)) continue;
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (HIDDEN.contains(key.getPath())) continue;
                ItemStack st = new ItemStack(item);
                if (st.isEmpty()) continue;
                ALL.add(new Entry(st, key.toString(), st.getHoverName().getString().toLowerCase(Locale.ROOT)));
            }
        }
        return ALL;
    }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        W = Math.min(320, width - 24);
        H = Math.min(230, height - 30);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        cols = Math.max(4, (W - 2 * PAD) / CELL);
        rows = Math.max(2, (H - 66) / CELL);
        search = addRenderableWidget(BinoUi.editBox(font, L + PAD, T + 32, W - 2 * PAD, 16, "Buscar bloco"));
        search.setMaxLength(40);
        search.setResponder(s -> refilter());
        addRenderableWidget(BinoUi.closeButton(L + W - 30, T - 8, 19, this::onClose));
        setInitialFocus(search);
        refilter();
    }

    private void refilter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Entry e : all()) if (q.isEmpty() || e.name().contains(q) || e.id().contains(q)) shown.add(e);
        scroll = 0;
    }

    private int maxScroll() { return Math.max(0, (shown.size() + cols - 1) / cols - rows); }

    private int cellX(int c) { return L + PAD + c * CELL + (W - 2 * PAD - cols * CELL) / 2; }

    private int cellY(int r) { return T + 56 + r * CELL; }

    @Nullable
    private Entry entryAt(double mx, double my) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = cellX(c), y = cellY(r);
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                    int i = (scroll + r) * cols + c;
                    return i < shown.size() ? shown.get(i) : null;
                }
            }
        }
        return null;
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L, T, W, H);
        BinoUi.field(g, search);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, "Escolher bloco", L + W / 2, T - 6);
        g.drawString(font, "Busca", L + PAD, T + 20, BinoUi.TEXT, false);
        Entry hover = entryAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = (scroll + r) * cols + c;
                if (i >= shown.size()) break;
                Entry e = shown.get(i);
                int x = cellX(c), y = cellY(r);
                boolean sel = ("item:" + e.id()).equals(current);
                if (sel) BinoUi.round(g, x, y, CELL, CELL, 0xFFFFFFFF);
                if (e == hover || sel) BinoUi.round(g, x + (sel ? 1 : 0), y + (sel ? 1 : 0), CELL - (sel ? 2 : 0), CELL - (sel ? 2 : 0), sel ? 0xFF2A2D2B : 0xFF484A49);
                g.renderItem(e.stack(), x + 2, y + 2);
            }
        }
        if (shown.isEmpty()) g.drawCenteredString(font, "Nenhum bloco encontrado", L + W / 2, T + H / 2, 0xFFAAAAAA);
        // barra de rolagem fina
        if (maxScroll() > 0) {
            int bx = L + W - 9, by = T + 56, bh = rows * CELL;
            g.fill(bx, by, bx + 3, by + bh, 0x40FFFFFF);
            int th = Math.max(12, bh * rows / ((shown.size() + cols - 1) / cols));
            int ty = by + (bh - th) * scroll / maxScroll();
            g.fill(bx, ty, bx + 3, ty + th, 0xC0FFFFFF);
        }
        if (hover != null) setTooltipForNextRenderPass(Component.literal(hover.stack().getHoverName().getString()));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(sy)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        Entry e = entryAt(mx, my);
        if (e != null && button == 0) {
            UiSound.tick();
            onPick.accept("item:" + e.id());
            onClose();
            return true;
        }
        return false;
    }
}
