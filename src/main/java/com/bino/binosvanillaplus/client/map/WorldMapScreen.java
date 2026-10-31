package com.bino.binosvanillaplus.client.map;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

// Mapa-mundi (layout do Figma "World Map"): o mapa 3D e' a propria janela. Arrastar = mover, roda = zoom, botao direito = menu de marcacoes,
// clique esquerdo numa marcacao = editar. M ou Esc = fechar.
public class WorldMapScreen extends Screen {
    private record Mark(int x, int z, String label) {}
    private record MenuItem(String label, Runnable action) {}

    private int mapX, mapY, mapW, mapH;    // area do mapa (o mapa e' a propria janela, sem moldura)
    private double cx, cz;
    private double zoom = 1.0;
    private final List<Mark> marks = new ArrayList<>();
    private long lastMarkScan = -1000;
    private final boolean mode3D = true;   // o mapa-mundi e' sempre 3D
    private boolean rDown, rMoved;
    private double rStartX, rStartY;
    private boolean help = false;          // painel "Help" aberto
    private MapUi.FlatButton helpClose;

    // menu de contexto
    private final List<MenuItem> menu = new ArrayList<>();
    private int menuX, menuY, menuW;
    private static final int MENU_ROW = 13, MENU_HEAD = 14;

    public WorldMapScreen() {
        super(Component.literal("World Map"));
        MapOptions.ensureLoaded();
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) { cx = p.getX(); cz = p.getZ(); }
    }

    @Override
    protected void init() {
        mapW = Math.max(280, Math.min(width - 24, (int) (width * 0.735)));
        mapH = Math.max(180, Math.min(height - 34, (int) (height * 0.77)));
        mapX = (width - mapW) / 2;
        mapY = (height - mapH) / 2 + 4;

        addRenderableWidget(MapUi.closeButton(mapX + mapW - 8, mapY - 6, 13, this::onClose));
        int by = mapY + mapH - 12, bx = mapX + mapW - 10 - 4 * 24 + 5;
        addRenderableWidget(new MapUi.FlatButton(bx, by, 19, 19, "", MapUi.Style.LIGHT,
                () -> Minecraft.getInstance().setScreen(new WaypointListScreen(this))).icon(MapUi.tex("waypoints"), 17))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Marcações")));
        addRenderableWidget(new MapUi.FlatButton(bx + 24, by, 19, 19, "", MapUi.Style.LIGHT,
                () -> Minecraft.getInstance().setScreen(new MapOptionsScreen(this))).icon(MapUi.tex("options"), 17))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Opções")));
        addRenderableWidget(new MapUi.FlatButton(bx + 48, by, 19, 19, "", MapUi.Style.LIGHT, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) { cx = p.getX(); cz = p.getZ(); }
        }).icon(MapUi.tex("center"), 17))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Centralizar no jogador")));
        addRenderableWidget(new MapUi.FlatButton(bx + 72, by, 19, 19, "?", MapUi.Style.LIGHT, () -> help = !help))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Ajuda")));
        int[] hr = helpRect();
        helpClose = addRenderableWidget(MapUi.closeButton(hr[0] + hr[2] - 7, hr[1] - 5, 13, () -> help = false));
        helpClose.visible = false;
    }

    // painel de ajuda: {x, y, w, h}
    private int[] helpRect() {
        int hw = Math.min(266, mapW - 24), hh = 104;
        return new int[]{mapX + mapW - hw - 10, mapY + mapH - hh - 22, hw, hh};
    }

    private static final String[] HELP = {
            "Para mover a tela: arraste com o botão esquerdo para os cantos.",
            "Para girar a tela: arraste com o botão direito para os cantos.",
            "Para dar zoom: suba ou desça a roda do mouse.",
            "Para acessar o menu: aperte o botão direito."};

    // ---------------------------------------------------------------- coordenadas

    private double toWorldX(double sx) { return cx + (sx - (mapX + mapW / 2.0)) / zoom; }
    private double toWorldZ(double sy) { return cz + (sy - (mapY + mapH / 2.0)) / zoom; }
    private float toScreenX(double wx) { return (float) (mapX + mapW / 2.0 + (wx - cx) * zoom); }
    private float toScreenY(double wz) { return (float) (mapY + mapH / 2.0 + (wz - cz) * zoom); }
    // posição na tela de um ponto do mundo (2D ou 3D); [2] == 0 -> fora/atrás da câmera
    private float[] screenOf(double wx, double wy, double wz) {
        if (!mode3D) return new float[]{toScreenX(wx), toScreenY(wz), 1f};
        return DioramaRenderer.projectWith(WorldMap3D.view, WorldMap3D.proj, (float) (wx - cx), (float) (wy - WorldMap3D.targetY),
                (float) (wz - cz), mapX, mapY, mapW, mapH);
    }

    // coordenadas de mundo sob o cursor (null se o cursor não está sobre terreno explorado, no 3D)
    private int[] pickWorld(double mx, double my) {
        if (!mode3D) return new int[]{(int) Math.floor(toWorldX(mx)), (int) Math.floor(toWorldZ(my))};
        return WorldMap3D.pick(mx, my, mapX, mapY, mapW, mapH, cx, cz);
    }

    private boolean inMap(double mx, double my) { return mx >= mapX && mx < mapX + mapW && my >= mapY && my < mapY + mapH; }

    private void scanMarks() {
        marks.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        List<ItemStack> stacks = new ArrayList<>(mc.player.getInventory().items);
        stacks.add(mc.player.getOffhandItem());
        for (ItemStack s : stacks) {
            if (!s.is(net.minecraft.world.item.Items.FILLED_MAP)) continue;
            MapItemSavedData data = MapItem.getSavedData(s, mc.level);
            if (data == null || data.dimension != mc.level.dimension()) continue;
            for (MapDecoration d : data.getDecorations()) {
                String label = null;
                if (d.type().is(ResourceLocation.withDefaultNamespace("red_x"))) label = "Tesouro";
                else if (d.type().is(ResourceLocation.withDefaultNamespace("woodland_mansion"))) label = "Mansão";
                else if (d.type().is(ResourceLocation.withDefaultNamespace("monument"))) label = "Monumento";
                if (label == null) continue;
                int scale = 1 << data.scale;
                marks.add(new Mark(data.centerX + (int) (d.x() / 2.0 * scale), data.centerZ + (int) (d.y() / 2.0 * scale), label));
            }
        }
    }

    private MapWaypoints.Waypoint waypointAt(double mx, double my) {
        if (!MapOptions.worldPins) return null;
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (w.hidden) continue;
            float[] s = screenOf(w.x + 0.5, w.y, w.z + 0.5);
            if (s[2] != 0f && Math.abs(mx - s[0]) <= 6 && Math.abs(my - s[1]) <= 6) return w;
        }
        return null;
    }

    // ---------------------------------------------------------------- desenho

    // vazio de propósito: o Screen.render do vanilla chamaria isto DEPOIS de eu desenhar o mapa (e aplica blur); o fundo é desenhado em render()
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getGameTime() - lastMarkScan > 20) { scanMarks(); lastMarkScan = mc.level.getGameTime(); }

        g.fill(0, 0, width, height, 0x60000000);   // escurece o jogo atras do mapa
        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0B0F14);

        LocalPlayer pl = mc.player;
        if (DioramaRenderer.ready()) WorldMap3D.render(g, mapX, mapY, mapW, mapH, cx, cz, pl);
        else g.drawCenteredString(font, "Shader do mapa indisponível", mapX + mapW / 2, mapY + mapH / 2, 0xFFFF8888);

        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        MapWaypoints.Waypoint hover = menu.isEmpty() && !help ? waypointAt(mx, my) : null;
        if (MapOptions.worldPins) {
            for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
                if (w.hidden) continue;
                float[] s = screenOf(w.x + 0.5, w.y, w.z + 0.5);
                if (s[2] == 0f) continue;
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 7, sy - 7, sx + 8, sy + 8, w == hover ? 0xFFFFFFFF : 0xFF000000);
                WaypointIcons.badge(g, w.icon(), sx - 6, sy - 6, 13, w.color);
                if (MapOptions.worldLabels) {
                    String label = w.name;
                    if (MapOptions.showDistance && pl != null)
                        label += " (" + (int) Math.sqrt(pl.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + " m)";
                    g.fill(sx + 9, sy - 6, sx + 13 + font.width(label), sy + 6, 0x80000000);
                    g.drawString(font, label, sx + 11, sy - 4, 0xFFFFFFFF, false);
                }
            }
        }
        for (Mark m : marks) {
            int h0 = ExploredMap.heightAt(m.x(), m.z());
            float[] s = screenOf(m.x() + 0.5, h0 == ExploredMap.UNKNOWN ? 64 : h0 + 1, m.z() + 0.5);
            if (s[2] == 0f) continue;
            g.drawCenteredString(font, "X", (int) s[0], (int) s[1] - 4, 0xFFFF2222);
            g.drawString(font, m.label(), (int) s[0] + 7, (int) s[1] - 4, 0xFFFFC0C0, true);
        }
        g.disableScissor();

        // barra de baixo: dia/hora e coordenadas do jogador
        if (mc.level != null && pl != null) {
            String day = dayTime(mc.level.getDayTime());
            String pos = "X: " + pl.getBlockX() + "  Y: " + pl.getBlockY() + "  Z: " + pl.getBlockZ();
            int ty = mapY + mapH - 13;
            int w1 = font.width(day), w2 = font.width(pos);
            g.fill(mapX, ty - 3, mapX + 4 + w1 + 6, ty + 11, 0x66000000);
            g.fill(mapX + 4 + w1 + 14, ty - 3, mapX + 4 + w1 + 14 + w2 + 8, ty + 11, 0x66000000);
            g.drawString(font, day, mapX + 4, ty, 0xFFFFFFFF, false);
            g.drawString(font, pos, mapX + 4 + w1 + 18, ty, 0xFFFFFFFF, false);
        }

        helpClose.visible = help;
        super.render(g, mx, my, pt);
        MapUi.title(g, font, "World Map", mapX + mapW / 2, mapY - 7);

        if (help) drawHelp(g);
        if (hover != null) {
            g.renderTooltip(font, Component.literal(hover.name + " (" + hover.x + ", " + hover.y + ", " + hover.z + ")  [" + hover.setName() + "]"), mx, my);
        }
        drawMenu(g, mx, my);
    }

    // "Day 0, 10:00 AM" (o dia do Minecraft comeca as 6:00)
    static String dayTime(long dayTime) {
        long day = dayTime / 24000L;
        int t = (int) ((dayTime + 6000L) % 24000L);
        int h = t / 1000, m = (t % 1000) * 60 / 1000;
        String ampm = h < 12 ? "AM" : "PM";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return "Day " + day + ", " + h12 + ":" + (m < 10 ? "0" : "") + m + " " + ampm;
    }

    private void drawHelp(GuiGraphics g) {
        int[] r = helpRect();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        MapUi.round(g, r[0], r[1], r[2], r[3], MapUi.MENU_BG);
        MapUi.titleLeft(g, font, "Help", r[0] + 12, r[1] - 7);
        int y = r[1] + 14;
        for (String line : HELP) {
            for (net.minecraft.util.FormattedCharSequence seq : font.split(Component.literal(line), r[2] - 18)) {
                g.drawString(font, seq, r[0] + 8, y, 0xFFFFFFFF, false);
                y += 10;
            }
            y += 4;
        }
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- menu de contexto

    private void openMenu(double mx, double my) {
        menu.clear();
        MapWaypoints.Waypoint w = waypointAt(mx, my);
        int[] at = pickWorld(mx, my);
        if (w == null && at == null) return;
        int wx = at == null ? w.x : at[0], wz = at == null ? w.z : at[1];
        int h = ExploredMap.heightAt(wx, wz);
        int wy = h == ExploredMap.UNKNOWN ? 64 : h + 1;
        Minecraft mc = Minecraft.getInstance();
        if (w != null) {
            menu.add(new MenuItem("Editar marcação", () -> mc.setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z))));
            menu.add(new MenuItem(w.hidden ? "Mostrar" : "Ocultar", () -> { w.hidden = !w.hidden; MapWaypoints.save(); }));
            menu.add(new MenuItem("Excluir", () -> MapWaypoints.remove(w)));
            menu.add(new MenuItem("Copiar coordenadas", () -> mc.keyboardHandler.setClipboard(w.x + " " + w.y + " " + w.z)));
            if (canTeleport()) menu.add(new MenuItem("Teleportar", () -> teleport(w.x, w.y, w.z)));
        } else {
            menu.add(new MenuItem("Criar marcação aqui", () -> mc.setScreen(new WaypointEditScreen(this, null, wx, wy, wz))));
            menu.add(new MenuItem("Marcação rápida aqui", () -> MapWaypoints.add(wx, wy, wz)));
            menu.add(new MenuItem("Copiar coordenadas", () -> mc.keyboardHandler.setClipboard(wx + " " + wy + " " + wz)));
            menu.add(new MenuItem("Centralizar aqui", () -> { cx = wx; cz = wz; }));
            if (canTeleport()) menu.add(new MenuItem("Teleportar", () -> teleport(wx, wy, wz)));
        }
        int widest = 0;
        for (MenuItem it : menu) widest = Math.max(widest, font.width(it.label()));
        menuW = Math.max(widest + 16, 70);
        menuX = (int) Math.max(2, Math.min(mx, width - menuW - 2));
        menuY = (int) Math.max(10, Math.min(my, height - menuHeight() - 2));
    }

    private int menuHeight() { return MENU_HEAD + menu.size() * MENU_ROW + 5; }

    private int menuRowY(int i) { return menuY + MENU_HEAD + i * MENU_ROW; }

    // teleporte so aparece no modo criativo
    private static boolean canTeleport() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.isCreative();
    }

    private void teleport(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand("tp @s " + x + " " + y + " " + z);   // exige permissao de operador
        onClose();
    }

    private void drawMenu(GuiGraphics g, int mx, int my) {
        if (menu.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        MapUi.round(g, menuX, menuY, menuW, menuHeight(), MapUi.MENU_BG);
        MapUi.titleLeft(g, font, "Menu", menuX + 10, menuY - 7);
        for (int i = 0; i < menu.size(); i++) {
            int y = menuRowY(i);
            boolean over = mx >= menuX && mx < menuX + menuW && my >= y && my < y + MENU_ROW;
            if (over) g.fill(menuX + 2, y, menuX + menuW - 2, y + MENU_ROW, 0x30FFFFFF);
            g.drawString(font, menu.get(i).label(), menuX + 7, y + 3, 0xFFFFFFFF, false);
        }
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!menu.isEmpty()) {
            for (int i = 0; i < menu.size(); i++) {
                int y = menuRowY(i);
                if (mx >= menuX && mx < menuX + menuW && my >= y && my < y + MENU_ROW) {
                    Runnable r = menu.get(i).action();
                    menu.clear();
                    r.run();
                    return true;
                }
            }
            menu.clear();
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        if (help) {   // clicar no painel de ajuda nao mexe no mapa; clicar fora fecha
            int[] r = helpRect();
            if (!(mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3])) help = false;
            return true;
        }
        if (!inMap(mx, my)) return false;
        if (button == 1) { rDown = true; rMoved = false; rStartX = mx; rStartY = my; return true; }
        if (button == 0) {
            MapWaypoints.Waypoint w = waypointAt(mx, my);
            if (w != null) {
                Minecraft.getInstance().setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!menu.isEmpty()) return super.mouseDragged(mx, my, button, dx, dy);
        if (button == 0) {
            if (!mode3D) {
                cx -= dx / zoom;
                cz -= dy / zoom;
            } else {
                double k = WorldMap3D.dist / Math.max(1, mapH) * 0.93;
                double sy = Math.sin(WorldMap3D.yaw), cyw = Math.cos(WorldMap3D.yaw);
                cx += (sy * dy - cyw * dx) * k;      // frente = (sin, -cos), direita = (cos, sin)
                cz += (-cyw * dy - sy * dx) * k;
            }
            return true;
        }
        if (button == 1 && rDown) {
            if (Math.abs(mx - rStartX) + Math.abs(my - rStartY) > 4) rMoved = true;
            if (mode3D && rMoved) {
                WorldMap3D.yaw += dx * 0.01;
                WorldMap3D.pitch = (float) Math.max(0.25, Math.min(1.45, WorldMap3D.pitch + dy * 0.008));
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 1 && rDown) {
            rDown = false;
            if (!rMoved) openMenu(rStartX, rStartY);   // clique direito sem arrastar = menu
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) {
            if (mode3D) WorldMap3D.dist = (float) Math.max(18, Math.min(700, WorldMap3D.dist * (sy > 0 ? 0.85 : 1.18)));
            else zoom = Math.max(0.25, Math.min(10.0, zoom * (sy > 0 ? 1.25 : 0.8)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (MapClientEvents.OPEN_MAP.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
