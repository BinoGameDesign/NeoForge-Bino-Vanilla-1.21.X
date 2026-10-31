package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// Criar / editar uma marcacao (Figma "New Waypoint").
// UX: cores e icones crescem no hover e o contorno de selecao desliza ate o escolhido; o icone acompanha a cor;
// coordenada invalida balanca o painel e marca o campo em vermelho; Enter salva; Tab navega entre os campos.
public class WaypointEditScreen extends BinoScreen {
    private static final int PAD = 16;
    @Nullable private final MapWaypoints.Waypoint editing;
    private final MapWaypoints.Waypoint draft;
    private EditBox name, xB, yB, zB, setB;
    private int L, T, W, H, step;
    private final Tween[] colorHover = new Tween[16], iconHover = new Tween[16];
    private final Tween colorSel = new Tween(0f, 40f), iconSel = new Tween(0f, 40f);
    private final Tween badgeR = new Tween(0f, 60f), badgeG = new Tween(0f, 60f), badgeB = new Tween(0f, 60f);
    private EditBox invalid;
    private long invalidAt = -10000;

    // editing == null -> cria uma nova em (x,y,z)
    public WaypointEditScreen(@Nullable Screen parent, @Nullable MapWaypoints.Waypoint editing, int x, int y, int z) {
        super(editing == null ? "New Waypoint" : "Edit Waypoint", parent);
        this.editing = editing;
        if (editing != null) {
            draft = new MapWaypoints.Waypoint();
            draft.name = editing.name; draft.x = editing.x; draft.y = editing.y; draft.z = editing.z;
            draft.color = editing.color; draft.symbol = editing.symbol; draft.set = editing.set; draft.hidden = editing.hidden;
        } else {
            draft = MapWaypoints.create(x, y, z);
        }
        for (int i = 0; i < 16; i++) { colorHover[i] = new Tween(0f, 40f); iconHover[i] = new Tween(0f, 40f); }
        badgeR.snap((draft.color >> 16) & 0xFF); badgeG.snap((draft.color >> 8) & 0xFF); badgeB.snap(draft.color & 0xFF);
    }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 34);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        step = Math.min(20, (W - 2 * PAD) / 16);
        int inner = W - 2 * PAD;

        name = addRenderableWidget(BinoUi.editBox(font, L + PAD, T + 34, inner, 16, "Nome da marcação"));
        name.setMaxLength(32);
        name.setValue(draft.name == null ? "" : draft.name);

        int numW = (inner - 3 * 11 - 2 * 18) / 3;    // tres campos com a letra na frente
        int col = 11 + numW + 18;
        xB = num(L + PAD + 11, numW, draft.x);
        yB = num(L + PAD + col + 11, numW, draft.y);
        zB = num(L + PAD + 2 * col + 11, numW, draft.z);

        setB = addRenderableWidget(BinoUi.editBox(font, L + PAD, T + 90, inner, 16, MapWaypoints.DEFAULT_SET));
        setB.setMaxLength(24);
        setB.setValue(draft.setName());

        int bw = (inner - 6) / 3, by = T + H - 26;
        BinoUi.Button[] vis = new BinoUi.Button[1];
        vis[0] = addRenderableWidget(new BinoUi.Button(L + PAD, by, bw, 16, visText(), BinoUi.Style.LIGHT, () -> {
            draft.hidden = !draft.hidden;
            vis[0].setLabel(visText());
        }).tooltip("Mostrar ou ocultar nos mapas"));
        addRenderableWidget(new BinoUi.Button(L + PAD + bw + 3, by, bw, 16, "Position", BinoUi.Style.LIGHT, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) return;
            xB.setValue(String.valueOf(p.getBlockX())); yB.setValue(String.valueOf(p.getBlockY())); zB.setValue(String.valueOf(p.getBlockZ()));
            Toasts.show("Coordenadas da sua posição");
        }).tooltip("Usar minha posição atual"));
        addRenderableWidget(new BinoUi.Button(L + PAD + 2 * (bw + 3), by, inner - 2 * (bw + 3), 16, "Salvar", BinoUi.Style.LIGHT, this::save)
                .tooltip("Salvar (Enter)"));
        addRenderableWidget(BinoUi.closeButton(L + W - 30, T - 8, 19, this::onClose));
        setInitialFocus(name);
        colorSel.snap(swatchX(Math.max(0, colorIndex())));
        iconSel.snap(swatchX(Math.max(0, WaypointIcons.IDS.indexOf(draft.icon()))));
    }

    private EditBox num(int x, int w, int value) {
        EditBox b = addRenderableWidget(BinoUi.editBox(font, x, T + 56, w, 16, ""));
        b.setMaxLength(8);
        b.setFilter(s -> s.matches("-?\\d*"));   // so numeros
        b.setValue(String.valueOf(value));
        return b;
    }

    private String visText() { return draft.hidden ? "Oculto" : "Visível"; }

    @Nullable
    private Integer parse(EditBox b) {
        try { return Integer.parseInt(b.getValue().trim()); } catch (NumberFormatException e) { return null; }
    }

    private void save() {
        Integer x = parse(xB), y = parse(yB), z = parse(zB);
        EditBox bad = x == null ? xB : y == null ? yB : z == null ? zB : null;
        if (bad != null) {
            invalid = bad;
            invalidAt = Ease.now();
            shake();
            setFocused(bad);
            return;
        }
        MapWaypoints.Waypoint w = editing != null ? editing : draft;
        w.name = name.getValue().trim().isEmpty() ? "Marcador" : name.getValue().trim();
        w.x = x; w.y = y; w.z = z;
        w.symbol = draft.symbol;
        w.set = setB.getValue().trim().isEmpty() ? MapWaypoints.DEFAULT_SET : setB.getValue().trim();
        w.color = draft.color;
        w.hidden = draft.hidden;
        if (editing == null) MapWaypoints.add(w); else MapWaypoints.save();
        Toasts.show(editing == null ? "Marcação \"" + w.name + "\" criada" : "Marcação salva");
        onClose();
    }

    private int swatchX(int i) { return L + PAD + i * step; }

    private int colorIndex() {
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) if (MapWaypoints.PALETTE[i] == draft.color) return i;
        return -1;
    }

    // cor atual do icone (transicao suave quando troca a cor)
    private int animatedColor() {
        return 0xFF000000 | ((int) badgeR.get((draft.color >> 16) & 0xFF) << 16) | ((int) badgeG.get((draft.color >> 8) & 0xFF) << 8)
                | (int) badgeB.get(draft.color & 0xFF);
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L, T, W, H);
        BinoUi.field(g, name);
        BinoUi.field(g, xB);
        BinoUi.field(g, yB);
        BinoUi.field(g, zB);
        BinoUi.field(g, setB);
        // campo invalido: contorno vermelho que some aos poucos
        float inv = 1f - Ease.since(invalidAt, 1600);
        if (invalid != null && inv > 0) BinoUi.outline(g, invalid.getX() - 5, invalid.getY() - 4, invalid.getWidth() + 10, 16, Ease.alpha(0xFFFF5050, inv));
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, editing == null ? "New Waypoint" : "Edit Waypoint", L + W / 2, T - 6);
        g.drawString(font, "Nome", L + PAD, T + 22, BinoUi.TEXT, false);
        g.drawString(font, "X", xB.getX() - 5 - 9, T + 60, BinoUi.TEXT, false);
        g.drawString(font, "Y", yB.getX() - 5 - 9, T + 60, BinoUi.TEXT, false);
        g.drawString(font, "Z", zB.getX() - 5 - 9, T + 60, BinoUi.TEXT, false);
        g.drawString(font, "Grupo", L + PAD, T + 78, BinoUi.TEXT, false);

        int sz = step - 4;
        g.drawString(font, "Cor", L + PAD, T + 113, BinoUi.TEXT, false);
        int ci = colorIndex();
        if (ci >= 0) {   // contorno branco que desliza ate a cor escolhida
            float sx = colorSel.get(swatchX(ci));
            BinoUi.round(g, (int) sx - 2, T + 122, sz + 4, sz + 4, 0xFFFFFFFF);
        }
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int x = swatchX(i), y = T + 124;
            boolean over = mx >= x && mx < x + sz && my >= y && my < y + sz;
            float h = colorHover[i].get(over ? 1f : 0f);
            float grow = h * 2f;
            g.fill((int) (x - grow), (int) (y - grow), (int) (x + sz + grow), (int) (y + sz + grow), MapWaypoints.PALETTE[i]);
        }

        g.drawString(font, "Ícone", L + PAD, T + 146, BinoUi.TEXT, false);
        int ii = WaypointIcons.IDS.indexOf(draft.icon());
        if (ii >= 0) {
            float sx = iconSel.get(swatchX(ii));
            BinoUi.round(g, (int) sx - 2, T + 155, sz + 4, sz + 4, 0xFFFFFFFF);
        }
        int bc = animatedColor();
        String hoverName = null;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int x = swatchX(i), y = T + 157;
            boolean over = mx >= x && mx < x + sz && my >= y && my < y + sz;
            float h = iconHover[i].get(over ? 1f : 0f);
            float s = 1f + 0.18f * h;
            g.pose().pushPose();
            g.pose().translate(x + sz / 2f, y + sz / 2f, 0);
            g.pose().scale(s, s, 1f);
            WaypointIcons.badge(g, WaypointIcons.IDS.get(i), -sz / 2, -sz / 2, sz, bc);
            g.pose().popPose();
            if (over) hoverName = WaypointIcons.NAMES.get(i);
        }
        if (hoverName != null) setTooltipForNextRenderPass(Component.literal(hoverName));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int sz = step - 4;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int ix = swatchX(i), iy = T + 157;
            if (mx >= ix && mx < ix + sz && my >= iy && my < iy + sz) { draft.symbol = WaypointIcons.IDS.get(i); UiSound.tick(); return true; }
        }
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int sx = swatchX(i), sy = T + 124;
            if (mx >= sx && mx < sx + sz && my >= sy && my < sy + sz) { draft.color = MapWaypoints.PALETTE[i]; UiSound.tick(); return true; }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
