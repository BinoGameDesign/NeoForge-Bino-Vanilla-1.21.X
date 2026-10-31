package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Lista de marcacoes (Figma "Waypoints").
// UX: linhas entram em cascata, destaque desliza ate a linha sob o mouse, rolagem suave com barra arrastavel,
// filtro de grupo em lista suspensa animada, excluir com "Desfazer", duplo clique edita,
// teclado: / ou Ctrl+F busca, setas escolhem a linha, Enter edita, Delete exclui, Espaco mostra/oculta.
public class WaypointListScreen extends BinoScreen {
    private static final int ROW = 26, PAD = 16;
    private EditBox search;
    private BinoUi.Button groupBtn;
    private String groupFilter = null;      // null = todos os grupos
    private final PopupMenu groupMenu = new PopupMenu(null);
    private final SmoothScroll scroll = new SmoothScroll();
    private final Tween hlY = new Tween(0f, 35f), hlA = new Tween(0f, 50f);
    private final Map<String, Tween> btnHover = new HashMap<>();
    private int L, T, W, H, listX, listY, listW, listH;
    private int selected = -1;              // linha escolhida pelo teclado
    private long lastClickAt = 0;
    private int lastClickRow = -1;
    private List<MapWaypoints.Waypoint> rows = new ArrayList<>();
    private boolean swallowChar = false;

    public WaypointListScreen(@Nullable Screen parent) {
        super("Waypoints", parent);
    }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 34);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        int btnW = 63;
        int searchW = W - 2 * PAD - 2 * btnW - 4;

        search = addRenderableWidget(BinoUi.editBox(font, L + PAD, T + 18, searchW, 16, "Buscar..."));
        search.setResponder(s -> { scroll.reset(); selected = -1; refresh(); });
        addRenderableWidget(new BinoUi.Button(L + PAD + searchW + 2, T + 18, btnW, 16, "Novo", BinoUi.Style.LIGHT, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            int x = p == null ? 0 : p.getBlockX(), y = p == null ? 64 : p.getBlockY(), z = p == null ? 0 : p.getBlockZ();
            Minecraft.getInstance().setScreen(new WaypointEditScreen(this, null, x, y, z));
        }).tooltip("Nova marcação na sua posição"));
        groupBtn = addRenderableWidget(new BinoUi.Button(L + W - PAD - btnW, T + 18, btnW, 16, groupLabel(), BinoUi.Style.LIGHT, this::openGroups));
        addRenderableWidget(BinoUi.closeButton(L + W - 30, T - 8, 19, this::onClose));

        int bw = 106;
        addRenderableWidget(new BinoUi.Button(L + W - PAD - 2 * bw - 3, T + H - 27, bw, 16, "Mostrar todas", BinoUi.Style.LIGHT, () -> setAllHidden(false)));
        addRenderableWidget(new BinoUi.Button(L + W - PAD - bw, T + H - 27, bw, 16, "Ocultar todas", BinoUi.Style.LIGHT, () -> setAllHidden(true)));

        listX = L + PAD; listY = T + 40; listW = W - 2 * PAD; listH = H - 40 - 35;
        refresh();
    }

    private String groupLabel() {
        String s = groupFilter == null ? "Todos" : groupFilter;
        return font.width(s) > 56 ? font.plainSubstrByWidth(s, 50) + "…" : s;
    }

    private void openGroups() {
        if (groupMenu.isOpen()) { groupMenu.close(); return; }
        List<PopupMenu.Item> items = new ArrayList<>();
        items.add(new PopupMenu.Item("Todos", () -> setGroup(null), groupFilter == null));
        for (String s : MapWaypoints.sets()) items.add(new PopupMenu.Item(s, () -> setGroup(s), s.equals(groupFilter)));
        groupMenu.open(groupBtn.getX(), groupBtn.getY() + 18, items, groupBtn.getWidth(), width, height);
    }

    private void setGroup(@Nullable String g) {
        groupFilter = g;
        groupBtn.setLabel(groupLabel());
        scroll.reset();
        selected = -1;
        refresh();
    }

    private void setAllHidden(boolean hidden) {
        for (MapWaypoints.Waypoint w : rows) w.hidden = hidden;
        MapWaypoints.save();
        Toasts.show(hidden ? "Marcações ocultas" : "Marcações visíveis");
    }

    private void refresh() {
        rows = new ArrayList<>();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (groupFilter != null && !w.setName().equals(groupFilter)) continue;
            if (!q.isEmpty() && !w.name.toLowerCase(Locale.ROOT).contains(q) && !w.setName().toLowerCase(Locale.ROOT).contains(q)) continue;
            rows.add(w);
        }
        rows.sort((a, b) -> a.setName().equals(b.setName()) ? a.name.compareToIgnoreCase(b.name) : a.setName().compareToIgnoreCase(b.setName()));
        if (selected >= rows.size()) selected = rows.size() - 1;
        scroll.setBounds(listX, listY, listW, listH, rows.size() * ROW);
    }

    private void delete(MapWaypoints.Waypoint w) {
        MapWaypoints.remove(w);
        refresh();
        Toasts.show("\"" + w.name + "\" excluída", "Desfazer", () -> MapWaypoints.add(w));
    }

    private void toggle(MapWaypoints.Waypoint w) {
        w.hidden = !w.hidden;
        MapWaypoints.save();
    }

    private void edit(MapWaypoints.Waypoint w) {
        Minecraft.getInstance().setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z));
    }

    // botoes das linhas: x de cada um (0 visivel, 1 editar, 2 excluir)
    private int rowBtnX(int k) { return listX + listW - 10 - (3 - k) * 19 - (2 - k) * 4; }

    private int rowAt(double mx, double my) {
        if (mx < listX || mx >= listX + listW || my < listY || my >= listY + listH) return -1;
        int i = (int) ((my - listY + scroll.offset()) / ROW);
        return i >= 0 && i < rows.size() ? i : -1;
    }

    private float hover(String key, boolean on) {
        return btnHover.computeIfAbsent(key, k -> new Tween(0f, 45f)).get(on ? 1f : 0f);
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        refresh();
        BinoUi.panel(g, L, T, W, H);
        BinoUi.field(g, search);
        BinoUi.round(g, listX, listY, listW, listH, BinoUi.LIST_BG);

        boolean blocked = groupMenu.isOpen();
        LocalPlayer p = Minecraft.getInstance().player;
        float off = scroll.offset();
        g.enableScissor(listX, listY, listX + listW, listY + listH);

        // destaque que desliza ate a linha sob o mouse (ou a escolhida pelo teclado)
        int hov = blocked ? -1 : rowAt(mx, my);
        int target = hov >= 0 ? hov : selected;
        if (target >= 0) {
            float ty = listY + target * ROW - off;
            if (hlA.target() < 0.5f) hlY.snap(ty);
            hlY.to(ty);
        }
        float ha = hlA.get(target >= 0 ? 1f : 0f);
        if (ha > 0.01f) g.fill(listX, (int) hlY.get(), listX + listW, (int) hlY.get() + ROW, Ease.alpha(selected >= 0 && hov < 0 ? 0x22FEC400 : 0x16FFFFFF, ha));

        int first = Math.max(0, (int) (off / ROW)), last = Math.min(rows.size() - 1, (int) ((off + listH) / ROW));
        for (int i = first; i <= last; i++) {
            MapWaypoints.Waypoint w = rows.get(i);
            float st = stagger(i - first);
            int y = (int) (listY + i * ROW - off);
            int sx = (int) ((1f - st) * 10f);                      // entra deslizando da direita
            int rx = listX + sx;
            WaypointIcons.badge(g, w.icon(), rx + 10, y + 5, 17, w.hidden ? 0xFF555555 : w.color);
            String sub = w.setName() + " " + w.x + ", " + w.y + ", " + w.z;
            if (p != null) sub += " (" + (int) Math.sqrt(p.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + "m)";
            int textW = rowBtnX(0) - (listX + 33) - 6;
            g.drawString(font, font.plainSubstrByWidth(w.name, textW), rx + 33, y + 4, w.hidden ? 0xFF5E5E5E : BinoUi.TEXT_DIM, false);
            g.drawString(font, font.plainSubstrByWidth(sub, textW), rx + 33, y + 14, w.hidden ? 0xFF6E6E6E : 0xFFD6D6D6, false);
            String[] t = {w.hidden ? "-" : "o", "e", "x"};
            for (int k = 0; k < 3; k++) {
                int bx = rowBtnX(k) + sx;
                boolean over = !blocked && mx >= bx && mx < bx + 19 && my >= y + 4 && my < y + 22 && my >= listY && my < listY + listH;
                BinoUi.darkButton(g, font, bx, y + 4, 19, 18, t[k], hover(System.identityHashCode(w) + ":" + k, over));
            }
        }
        g.disableScissor();
        scroll.renderBar(g, mx, my);
        if (rows.isEmpty()) {
            String msg = MapWaypoints.list().isEmpty() ? "Nenhuma marcação ainda. Clique em Novo ou aperte H no jogo." : "Nada encontrado.";
            g.drawCenteredString(font, msg, listX + listW / 2, listY + listH / 2 - 4, BinoUi.TEXT_DIM);
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, "Waypoints", L + W / 2, T - 6);
        groupMenu.render(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isClosing()) return true;
        if (groupMenu.mouseClicked(mx, my)) return true;
        if (scroll.mouseClicked(mx, my)) return true;
        if (super.mouseClicked(mx, my, button)) return true;
        int i = rowAt(mx, my);
        if (i < 0 || button != 0) return false;
        MapWaypoints.Waypoint w = rows.get(i);
        int y = (int) (listY + i * ROW - scroll.offset()) + 4;
        if (my >= y && my < y + 18) {
            if (mx >= rowBtnX(0) && mx < rowBtnX(0) + 19) { UiSound.tick(); toggle(w); return true; }
            if (mx >= rowBtnX(1) && mx < rowBtnX(1) + 19) { UiSound.click(); edit(w); return true; }
            if (mx >= rowBtnX(2) && mx < rowBtnX(2) + 19) { UiSound.click(); delete(w); return true; }
        }
        // clique na linha: escolhe; duplo clique: edita
        long now = Ease.now();
        if (lastClickRow == i && now - lastClickAt < 300) { UiSound.click(); edit(w); return true; }
        lastClickRow = i; lastClickAt = now;
        selected = i;
        setFocused(null);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (scroll.mouseDragged(my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        scroll.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (groupMenu.isOpen()) return true;
        scroll.scrollBy((float) (-sy * ROW * 1.5));
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (swallowChar) { swallowChar = false; return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (groupMenu.keyPressed(key)) return true;
        boolean typing = search.isFocused();
        if (!typing) {
            if (key == GLFW.GLFW_KEY_SLASH || (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0)) {
                setFocused(search);
                search.setFocused(true);
                swallowChar = key == GLFW.GLFW_KEY_SLASH;   // nao digitar a "/" na busca
                return true;
            }
            if (!rows.isEmpty() && (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP)) {
                selected = Math.max(0, Math.min(rows.size() - 1, selected + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
                // mantem a linha escolhida visivel
                float top = selected * ROW, off = scroll.offset();
                if (top < off) scroll.scrollBy(top - off);
                else if (top + ROW > off + listH) scroll.scrollBy(top + ROW - off - listH);
                UiSound.tick();
                return true;
            }
            if (selected >= 0 && selected < rows.size()) {
                MapWaypoints.Waypoint w = rows.get(selected);
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { edit(w); return true; }
                if (key == GLFW.GLFW_KEY_DELETE) { delete(w); return true; }
                if (key == GLFW.GLFW_KEY_SPACE) { toggle(w); UiSound.tick(); return true; }
            }
        } else if (key == GLFW.GLFW_KEY_DOWN && !rows.isEmpty()) {   // da busca desce para a lista
            setFocused(null);
            search.setFocused(false);
            selected = 0;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
