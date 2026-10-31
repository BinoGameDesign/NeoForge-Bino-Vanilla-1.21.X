package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

// Criar / editar uma marcacao (layout do Figma "New Waypoint"): nome, X/Y/Z, grupo, cor (16), icone (16),
// Visivel / Position (usar minha posicao) / Salvar.
public class WaypointEditScreen extends Screen {
    private static final int PAD = 16;
    private final Screen parent;
    @Nullable private final MapWaypoints.Waypoint editing;
    private final MapWaypoints.Waypoint draft;
    private EditBox name, xB, yB, zB, setB;
    private int L, T, W, H, step;

    // editing == null -> cria uma nova em (x,y,z)
    public WaypointEditScreen(@Nullable Screen parent, @Nullable MapWaypoints.Waypoint editing, int x, int y, int z) {
        super(Component.literal(editing == null ? "New Waypoint" : "Edit Waypoint"));
        this.parent = parent;
        this.editing = editing;
        if (editing != null) {
            draft = new MapWaypoints.Waypoint();
            draft.name = editing.name; draft.x = editing.x; draft.y = editing.y; draft.z = editing.z;
            draft.color = editing.color; draft.symbol = editing.symbol; draft.set = editing.set; draft.hidden = editing.hidden;
        } else {
            draft = MapWaypoints.create(x, y, z);
        }
    }

    @Override
    protected void init() {
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 34);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        step = Math.min(20, (W - 2 * PAD) / 16);
        int inner = W - 2 * PAD;

        name = addRenderableWidget(MapUi.editBox(font, L + PAD, T + 34, inner, 16, ""));
        name.setMaxLength(32);
        name.setValue(draft.name == null ? "" : draft.name);

        int numW = (inner - 3 * 11 - 2 * 18) / 3;    // tres campos com a letra na frente
        int col = 11 + numW + 18;
        xB = num(L + PAD + 11, numW, draft.x);
        yB = num(L + PAD + col + 11, numW, draft.y);
        zB = num(L + PAD + 2 * col + 11, numW, draft.z);

        setB = addRenderableWidget(MapUi.editBox(font, L + PAD, T + 90, inner, 16, ""));
        setB.setMaxLength(24);
        setB.setValue(draft.setName());

        int bw = (inner - 6) / 3, by = T + H - 26;
        MapUi.FlatButton[] vis = new MapUi.FlatButton[1];
        vis[0] = addRenderableWidget(new MapUi.FlatButton(L + PAD, by, bw, 16, visText(), MapUi.Style.LIGHT, () -> {
            draft.hidden = !draft.hidden;
            vis[0].setLabel(visText());
        }));
        addRenderableWidget(new MapUi.FlatButton(L + PAD + bw + 3, by, bw, 16, "Position", MapUi.Style.LIGHT, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) return;
            xB.setValue(String.valueOf(p.getBlockX())); yB.setValue(String.valueOf(p.getBlockY())); zB.setValue(String.valueOf(p.getBlockZ()));
        }));
        addRenderableWidget(new MapUi.FlatButton(L + PAD + 2 * (bw + 3), by, inner - 2 * (bw + 3), 16, "Salvar", MapUi.Style.LIGHT, this::save));
        addRenderableWidget(MapUi.closeButton(L + W - 30, T - 8, 19, this::onClose));
    }

    private EditBox num(int x, int w, int value) {
        EditBox b = addRenderableWidget(MapUi.editBox(font, x, T + 56, w, 16, ""));
        b.setMaxLength(8);
        b.setValue(String.valueOf(value));
        return b;
    }

    private String visText() { return draft.hidden ? "Oculto" : "Visível"; }

    private static int parse(EditBox b, int fallback) {
        try { return Integer.parseInt(b.getValue().trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private void save() {
        MapWaypoints.Waypoint w = editing != null ? editing : draft;
        w.name = name.getValue().trim().isEmpty() ? "Marcador" : name.getValue().trim();
        w.x = parse(xB, draft.x); w.y = parse(yB, draft.y); w.z = parse(zB, draft.z);
        w.symbol = draft.symbol;
        w.set = setB.getValue().trim().isEmpty() ? MapWaypoints.DEFAULT_SET : setB.getValue().trim();
        w.color = draft.color;
        w.hidden = draft.hidden;
        if (editing == null) MapWaypoints.add(w); else MapWaypoints.save();
        onClose();
    }

    private int swatchX(int i) { return L + PAD + i * step; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x60000000);
        MapUi.panel(g, L, T, W, H);
        MapUi.field(g, name);
        MapUi.field(g, xB);
        MapUi.field(g, yB);
        MapUi.field(g, zB);
        MapUi.field(g, setB);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        MapUi.title(g, font, editing == null ? "New Waypoint" : "Edit Waypoint", L + W / 2, T - 6);
        g.drawString(font, "Nome", L + PAD, T + 22, 0xFFFFFFFF, false);
        g.drawString(font, "X", xB.getX() - 5 - 9, T + 60, 0xFFFFFFFF, false);
        g.drawString(font, "Y", yB.getX() - 5 - 9, T + 60, 0xFFFFFFFF, false);
        g.drawString(font, "Z", zB.getX() - 5 - 9, T + 60, 0xFFFFFFFF, false);
        g.drawString(font, "Grupo", L + PAD, T + 78, 0xFFFFFFFF, false);

        g.drawString(font, "Cor", L + PAD, T + 113, 0xFFFFFFFF, false);
        int sz = step - 4;
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int sx = swatchX(i), sy = T + 124;
            if (MapWaypoints.PALETTE[i] == draft.color) g.fill(sx - 1, sy - 1, sx + sz + 1, sy + sz + 1, 0xFFFFFFFF);
            g.fill(sx, sy, sx + sz, sy + sz, MapWaypoints.PALETTE[i]);
        }

        g.drawString(font, "Ícone", L + PAD, T + 146, 0xFFFFFFFF, false);
        String hoverName = null;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int ix = swatchX(i), iy = T + 157;
            boolean sel = WaypointIcons.IDS.get(i).equals(draft.icon());
            boolean over = mx >= ix && mx < ix + sz && my >= iy && my < iy + sz;
            if (sel) g.fill(ix - 1, iy - 1, ix + sz + 1, iy + sz + 1, 0xFFFFFFFF);
            WaypointIcons.badge(g, WaypointIcons.IDS.get(i), ix, iy, sz, over && !sel ? brighten(draft.color) : draft.color);
            if (over) hoverName = WaypointIcons.NAMES.get(i);
        }
        if (hoverName != null) g.renderTooltip(font, Component.literal(hoverName), mx, my);
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40), gg = Math.min(255, ((c >> 8) & 0xFF) + 40), b = Math.min(255, (c & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int sz = step - 4;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int ix = swatchX(i), iy = T + 157;
            if (mx >= ix && mx < ix + sz && my >= iy && my < iy + sz) { draft.symbol = WaypointIcons.IDS.get(i); return true; }
        }
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int sx = swatchX(i), sy = T + 124;
            if (mx >= sx && mx < sx + sz && my >= sy && my < sy + sz) { draft.color = MapWaypoints.PALETTE[i]; return true; }
        }
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
