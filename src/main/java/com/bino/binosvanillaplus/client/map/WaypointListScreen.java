package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Lista de marcacoes (layout do Figma "Waypoints"): busca, Novo, filtro de grupo (Todos), linhas com mostrar/editar/excluir,
// Mostrar todas / Ocultar todas.
public class WaypointListScreen extends Screen {
    private static final int ROW = 26, PAD = 16;
    @Nullable private final Screen parent;
    private EditBox search;
    private MapUi.FlatButton groupBtn;
    private String groupFilter = null;      // null = todos os grupos
    private boolean dropdown = false;
    private int scroll = 0, L, T, W, H, listX, listY, listW, listH;
    private List<MapWaypoints.Waypoint> rows = new ArrayList<>();

    public WaypointListScreen(@Nullable Screen parent) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 34);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        int btnW = 63;
        int searchW = W - 2 * PAD - 2 * btnW - 4;

        search = addRenderableWidget(MapUi.editBox(font, L + PAD, T + 18, searchW, 16, "Buscar..."));
        search.setResponder(s -> { scroll = 0; refresh(); });
        addRenderableWidget(new MapUi.FlatButton(L + PAD + searchW + 2, T + 18, btnW, 16, "Novo", MapUi.Style.LIGHT, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            int x = p == null ? 0 : p.getBlockX(), y = p == null ? 64 : p.getBlockY(), z = p == null ? 0 : p.getBlockZ();
            Minecraft.getInstance().setScreen(new WaypointEditScreen(this, null, x, y, z));
        }));
        groupBtn = addRenderableWidget(new MapUi.FlatButton(L + W - PAD - btnW, T + 18, btnW, 16, groupLabel(), MapUi.Style.LIGHT,
                () -> dropdown = !dropdown));
        addRenderableWidget(MapUi.closeButton(L + W - 30, T - 8, 19, this::onClose));

        int bw = 106;
        addRenderableWidget(new MapUi.FlatButton(L + W - PAD - 2 * bw - 3, T + H - 27, bw, 16, "Mostrar todas", MapUi.Style.LIGHT, () -> setAllHidden(false)));
        addRenderableWidget(new MapUi.FlatButton(L + W - PAD - bw, T + H - 27, bw, 16, "Ocultar todas", MapUi.Style.LIGHT, () -> setAllHidden(true)));

        listX = L + PAD; listY = T + 40; listW = W - 2 * PAD; listH = H - 40 - 35;
        refresh();
    }

    private String groupLabel() {
        String s = groupFilter == null ? "Todos" : groupFilter;
        return font.width(s) > 56 ? font.plainSubstrByWidth(s, 50) + "…" : s;
    }

    private List<String> groupOptions() {
        List<String> l = new ArrayList<>();
        l.add(null);
        l.addAll(MapWaypoints.sets());
        return l;
    }

    private void setAllHidden(boolean hidden) {
        for (MapWaypoints.Waypoint w : rows) w.hidden = hidden;
        MapWaypoints.save();
    }

    private void refresh() {
        rows = new ArrayList<>();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (groupFilter != null && !w.setName().equals(groupFilter)) continue;
            if (!q.isEmpty() && !w.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            rows.add(w);
        }
        rows.sort((a, b) -> a.setName().equals(b.setName()) ? a.name.compareToIgnoreCase(b.name) : a.setName().compareToIgnoreCase(b.setName()));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
    }

    private int visibleRows() { return Math.max(1, listH / ROW); }

    // botoes das linhas: x de cada um (visivel, editar, excluir)
    private int rowBtnX(int k) { return listX + listW - 8 - (3 - k) * 19 - (2 - k) * 4; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x60000000);
        MapUi.panel(g, L, T, W, H);
        MapUi.field(g, search);
        MapUi.round(g, listX, listY, listW, listH, MapUi.LIST_BG);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        refresh();
        super.render(g, mx, my, pt);
        MapUi.title(g, font, "Waypoints", L + W / 2, T - 6);

        LocalPlayer p = Minecraft.getInstance().player;
        g.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            MapWaypoints.Waypoint w = rows.get(scroll + i);
            int y = listY + i * ROW;
            boolean overRow = !dropdown && mx >= listX && mx < listX + listW && my >= y && my < y + ROW;
            if (overRow) g.fill(listX, y, listX + listW, y + ROW, 0x14FFFFFF);
            WaypointIcons.badge(g, w.icon(), listX + 10, y + 5, 17, w.hidden ? 0xFF555555 : w.color);
            String sub = w.setName() + " " + w.x + ", " + w.y + ", " + w.z;
            if (p != null) sub += " (" + (int) Math.sqrt(p.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + "m)";
            int textW = rowBtnX(0) - (listX + 33) - 6;
            g.drawString(font, font.plainSubstrByWidth(w.name, textW), listX + 33, y + 4, w.hidden ? 0xFF5E5E5E : 0xFF8A8A8A, false);
            g.drawString(font, font.plainSubstrByWidth(sub, textW), listX + 33, y + 14, w.hidden ? 0xFF6E6E6E : 0xFFD6D6D6, false);
            rowBtn(g, rowBtnX(0), y + 4, w.hidden ? "-" : "o", mx, my);
            rowBtn(g, rowBtnX(1), y + 4, "e", mx, my);
            rowBtn(g, rowBtnX(2), y + 4, "x", mx, my);
        }
        g.disableScissor();
        if (rows.isEmpty()) g.drawCenteredString(font, "Nenhuma marcação.", listX + listW / 2, listY + 20, 0xFF8A8A8A);

        // lista de grupos aberta pelo botao "Todos"
        if (dropdown) {
            List<String> opts = groupOptions();
            int dx = groupBtn.getX(), dy = groupBtn.getY() + 18, dw = Math.max(groupBtn.getWidth(), 90);
            dx = Math.min(dx, L + W - dw - 4);
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            MapUi.round(g, dx, dy, dw, opts.size() * 13 + 4, 0xFF2A2D2B);
            for (int i = 0; i < opts.size(); i++) {
                int oy = dy + 2 + i * 13;
                boolean over = mx >= dx && mx < dx + dw && my >= oy && my < oy + 13;
                if (over) g.fill(dx + 1, oy, dx + dw - 1, oy + 13, 0xFF454946);
                String s = opts.get(i) == null ? "Todos" : opts.get(i);
                boolean sel = (opts.get(i) == null && groupFilter == null) || (opts.get(i) != null && opts.get(i).equals(groupFilter));
                g.drawString(font, font.plainSubstrByWidth(s, dw - 10), dx + 5, oy + 3, sel ? 0xFFFEC400 : 0xFFE0E0E0, false);
            }
            g.pose().popPose();
        }
    }

    private void rowBtn(GuiGraphics g, int x, int y, String t, int mx, int my) {
        boolean over = !dropdown && mx >= x && mx < x + 19 && my >= y && my < y + 18;
        MapUi.round(g, x, y, 19, 18, 0xFF5B5E5C);
        MapUi.round(g, x + 1, y + 1, 17, 16, over ? MapUi.DARK_BTN_HOVER : MapUi.DARK_BTN);
        g.drawString(font, t, x + 10 - font.width(t) / 2, y + 5, 0xFFB8B8B8, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dropdown) {
            List<String> opts = groupOptions();
            int dx = groupBtn.getX(), dy = groupBtn.getY() + 18, dw = Math.max(groupBtn.getWidth(), 90);
            dx = Math.min(dx, L + W - dw - 4);
            for (int i = 0; i < opts.size(); i++) {
                int oy = dy + 2 + i * 13;
                if (mx >= dx && mx < dx + dw && my >= oy && my < oy + 13) {
                    groupFilter = opts.get(i);
                    groupBtn.setLabel(groupLabel());
                    scroll = 0;
                    refresh();
                    break;
                }
            }
            dropdown = false;
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || mx < listX || mx >= listX + listW || my < listY || my >= listY + listH) return false;
        int i = (int) ((my - listY) / ROW);
        if (scroll + i >= rows.size()) return false;
        MapWaypoints.Waypoint w = rows.get(scroll + i);
        int y = listY + i * ROW + 4;
        if (my >= y && my < y + 18) {
            if (mx >= rowBtnX(0) && mx < rowBtnX(0) + 19) { w.hidden = !w.hidden; MapWaypoints.save(); return true; }
            if (mx >= rowBtnX(1) && mx < rowBtnX(1) + 19) {
                Minecraft.getInstance().setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z));
                return true;
            }
            if (mx >= rowBtnX(2) && mx < rowBtnX(2) + 19) { MapWaypoints.remove(w); refresh(); return true; }
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
