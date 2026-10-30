package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

// Criar / editar uma marcacao: nome, coordenadas, simbolo, grupo, cor (16), visivel.
public class WaypointEditScreen extends Screen {
    private final Screen parent;
    @Nullable private final MapWaypoints.Waypoint editing;
    private final MapWaypoints.Waypoint draft;
    private EditBox name, xB, yB, zB, setB;
    private Button visBtn;
    private int top, left;
    private static final int W = 240, H = 262;

    // editing == null -> cria uma nova em (x,y,z)
    public WaypointEditScreen(@Nullable Screen parent, @Nullable MapWaypoints.Waypoint editing, int x, int y, int z) {
        super(Component.literal("Marcação"));
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
        left = (width - W) / 2;
        top = (height - H) / 2;
        int fx = left + 10;

        name = addRenderableWidget(new EditBox(font, fx, top + 30, W - 20, 16, Component.literal("Nome")));
        name.setMaxLength(32);
        name.setValue(draft.name == null ? "" : draft.name);

        xB = num(fx, top + 62, draft.x);
        yB = num(fx + 74, top + 62, draft.y);
        zB = num(fx + 148, top + 62, draft.z);

        setB = addRenderableWidget(new EditBox(font, fx, top + 94, W - 20, 16, Component.literal("Grupo")));
        setB.setMaxLength(24);
        setB.setValue(draft.setName());

        visBtn = addRenderableWidget(Button.builder(visText(), b -> { draft.hidden = !draft.hidden; b.setMessage(visText()); })
                .bounds(fx, top + 218, 100, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Usar minha posição"), b -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) return;
            xB.setValue(String.valueOf(p.getBlockX())); yB.setValue(String.valueOf(p.getBlockY())); zB.setValue(String.valueOf(p.getBlockZ()));
        }).bounds(fx + 106, top + 218, W - 20 - 106, 18).build());

        int by = top + H - 24;
        addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> save()).bounds(fx, by, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose()).bounds(fx + 76, by, 70, 18).build());
        if (editing != null) {
            addRenderableWidget(Button.builder(Component.literal("Excluir"), b -> {
                MapWaypoints.remove(editing);
                onClose();
            }).bounds(fx + 152, by, W - 20 - 152, 18).build());
        }
    }

    private EditBox num(int x, int y, int value) {
        EditBox b = addRenderableWidget(new EditBox(font, x, y, 66, 16, Component.literal("n")));
        b.setMaxLength(8);
        b.setValue(String.valueOf(value));
        return b;
    }

    private Component visText() {
        return Component.literal(draft.hidden ? "Oculto" : "Visível");
    }

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

    // fundo + painel (o Screen.render chama isto antes dos widgets; sobrescrever evita o blur)
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000);
        g.fill(left, top, left + W, top + H, 0xFF2A2A38);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int fx = left + 10;
        g.drawString(font, editing == null ? "Nova marcação" : "Editar marcação", fx, top + 8, 0xFFFFFFFF, true);
        g.drawString(font, "Nome", fx, top + 20, 0xFFAAAAAA, false);
        g.drawString(font, "X", fx, top + 52, 0xFFAAAAAA, false);
        g.drawString(font, "Y", fx + 74, top + 52, 0xFFAAAAAA, false);
        g.drawString(font, "Z", fx + 148, top + 52, 0xFFAAAAAA, false);
        g.drawString(font, "Grupo", fx, top + 84, 0xFFAAAAAA, false);
        g.drawString(font, "Ícone", fx, top + 116, 0xFFAAAAAA, false);
        String hoverName = null;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int ix = fx + (i % 8) * 28, iy = top + 126 + (i / 8) * 22;
            boolean sel = WaypointIcons.IDS.get(i).equals(draft.icon());
            boolean over = mx >= ix && mx < ix + 22 && my >= iy && my < iy + 20;
            g.fill(ix - 1, iy - 1, ix + 23, iy + 21, sel ? 0xFFFFFFFF : 0xFF000000);
            g.fill(ix, iy, ix + 22, iy + 20, over ? 0xFF55557A : draft.color);
            WaypointIcons.draw(g, WaypointIcons.IDS.get(i), ix + 3, iy + 2, 16);
            if (over) hoverName = WaypointIcons.NAMES.get(i);
        }
        if (hoverName != null) g.renderTooltip(font, Component.literal(hoverName), mx, my);
        g.drawString(font, "Cor", fx, top + 172, 0xFFAAAAAA, false);
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int sx = fx + (i % 8) * 28, sy = top + 182 + (i / 8) * 14;
            boolean sel = MapWaypoints.PALETTE[i] == draft.color;
            g.fill(sx - 1, sy - 1, sx + 23, sy + 11, sel ? 0xFFFFFFFF : 0xFF000000);
            g.fill(sx, sy, sx + 22, sy + 10, MapWaypoints.PALETTE[i]);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int fx = left + 10;
        for (int i = 0; i < WaypointIcons.IDS.size(); i++) {
            int ix = fx + (i % 8) * 28, iy = top + 126 + (i / 8) * 22;
            if (mx >= ix && mx < ix + 22 && my >= iy && my < iy + 20) { draft.symbol = WaypointIcons.IDS.get(i); return true; }
        }
        for (int i = 0; i < MapWaypoints.PALETTE.length; i++) {
            int sx = fx + (i % 8) * 28, sy = top + 182 + (i / 8) * 14;
            if (mx >= sx && mx < sx + 22 && my >= sy && my < sy + 10) { draft.color = MapWaypoints.PALETTE[i]; return true; }
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
