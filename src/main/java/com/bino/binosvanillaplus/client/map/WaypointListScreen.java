package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Lista de marcacoes: busca, filtro por grupo, mostrar/ocultar, editar, excluir, ir ate (centraliza o mapa).
public class WaypointListScreen extends Screen {
    private static final int W = 300, ROW = 24;
    @Nullable private final Screen parent;
    private EditBox search;
    private Button setBtn;
    private String setFilter = null;      // null = todos
    private int scroll = 0, left, top, H, listTop, listBottom;
    private List<MapWaypoints.Waypoint> rows = new ArrayList<>();

    public WaypointListScreen(@Nullable Screen parent) {
        super(Component.literal("Marcações"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        H = Math.min(height - 20, 240);
        left = (width - W) / 2;
        top = (height - H) / 2;
        int fx = left + 8;
        search = addRenderableWidget(new EditBox(font, fx, top + 22, 130, 16, Component.literal("Buscar")));
        search.setHint(Component.literal("Buscar..."));
        search.setResponder(s -> { scroll = 0; refresh(); });
        setBtn = addRenderableWidget(Button.builder(setText(), b -> cycleSet()).bounds(fx + 134, top + 22, 80, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Novo"), b -> {
            LocalPlayer p = Minecraft.getInstance().player;
            int x = p == null ? 0 : p.getBlockX(), y = p == null ? 64 : p.getBlockY(), z = p == null ? 0 : p.getBlockZ();
            Minecraft.getInstance().setScreen(new WaypointEditScreen(this, null, x, y, z));
        }).bounds(fx + 218, top + 22, 66, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose()).bounds(left + W - 78, top + H - 22, 70, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Mostrar todas"), b -> setAllHidden(false)).bounds(left + 8, top + H - 22, 82, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Ocultar todas"), b -> setAllHidden(true)).bounds(left + 94, top + H - 22, 82, 16).build());
        listTop = top + 44;
        listBottom = top + H - 28;
        refresh();
    }

    private Component setText() { return Component.literal(setFilter == null ? "Todos" : setFilter); }

    private void cycleSet() {
        List<String> sets = new ArrayList<>(MapWaypoints.sets());
        if (setFilter == null) setFilter = sets.get(0);
        else {
            int i = sets.indexOf(setFilter) + 1;
            setFilter = i >= sets.size() ? null : sets.get(i);
        }
        setBtn.setMessage(setText());
        scroll = 0;
        refresh();
    }

    private void setAllHidden(boolean hidden) {
        for (MapWaypoints.Waypoint w : rows) w.hidden = hidden;
        MapWaypoints.save();
    }

    private void refresh() {
        rows = new ArrayList<>();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (setFilter != null && !w.setName().equals(setFilter)) continue;
            if (!q.isEmpty() && !w.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            rows.add(w);
        }
        rows.sort((a, b) -> a.setName().equals(b.setName()) ? a.name.compareToIgnoreCase(b.name) : a.setName().compareToIgnoreCase(b.setName()));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
    }

    private int visibleRows() { return Math.max(1, (listBottom - listTop) / ROW); }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000);
        g.fill(left, top, left + W, top + H, 0xFF2A2A38);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        refresh();
        super.render(g, mx, my, pt);
        g.drawString(font, "Marcações (" + rows.size() + ")", left + 8, top + 8, 0xFFFFFFFF, true);
        g.fill(left + 6, listTop - 1, left + W - 6, listBottom + 1, 0xFF000000);
        g.fill(left + 7, listTop, left + W - 7, listBottom, 0xFF181822);

        LocalPlayer p = Minecraft.getInstance().player;
        g.enableScissor(left + 7, listTop, left + W - 7, listBottom);
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            MapWaypoints.Waypoint w = rows.get(scroll + i);
            int y = listTop + i * ROW, x = left + 9;
            if (mx >= left + 7 && mx < left + W - 7 && my >= y && my < y + ROW) g.fill(left + 7, y, left + W - 7, y + ROW, 0x30FFFFFF);
            // icone
            g.fill(x, y + 3, x + 18, y + 21, 0xFF000000);
            g.fill(x + 1, y + 4, x + 17, y + 20, w.hidden ? 0xFF555555 : w.color);
            g.drawCenteredString(font, w.symbolOrInitial(), x + 9, y + 8, 0xFFFFFFFF);
            // texto
            int tc = w.hidden ? 0xFF888888 : 0xFFFFFFFF;
            g.drawString(font, font.plainSubstrByWidth(w.name, 120), x + 24, y + 3, tc, false);
            String sub = w.setName() + "  " + w.x + ", " + w.y + ", " + w.z;
            if (p != null) sub += "  (" + (int) Math.sqrt(p.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + "m)";
            g.drawString(font, font.plainSubstrByWidth(sub, 170), x + 24, y + 13, 0xFF9A9AAA, false);
            // botoes
            btn(g, left + W - 66, y + 5, w.hidden ? "o" : "O", mx, my);
            btn(g, left + W - 48, y + 5, "E", mx, my);
            btn(g, left + W - 30, y + 5, "X", mx, my);
        }
        g.disableScissor();
        if (rows.isEmpty()) g.drawCenteredString(font, "Nenhuma marcação.", left + W / 2, listTop + 20, 0xFFAAAAAA);
    }

    private void btn(GuiGraphics g, int x, int y, String t, int mx, int my) {
        boolean over = mx >= x && mx < x + 15 && my >= y && my < y + 14;
        g.fill(x, y, x + 15, y + 14, over ? 0xFF6B6B8A : 0xFF3A3A50);
        g.drawCenteredString(font, t, x + 8, y + 3, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || my < listTop || my >= listBottom) return false;
        int i = (int) ((my - listTop) / ROW);
        if (scroll + i >= rows.size()) return false;
        MapWaypoints.Waypoint w = rows.get(scroll + i);
        int y = listTop + i * ROW + 5;
        if (my >= y && my < y + 14) {
            if (mx >= left + W - 66 && mx < left + W - 51) { w.hidden = !w.hidden; MapWaypoints.save(); return true; }
            if (mx >= left + W - 48 && mx < left + W - 33) {
                Minecraft.getInstance().setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z));
                return true;
            }
            if (mx >= left + W - 30 && mx < left + W - 15) { MapWaypoints.remove(w); refresh(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - visibleRows()), scroll - (int) Math.signum(sy)));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
