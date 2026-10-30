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

// Mapa-mundi em janela (nao ocupa a tela toda). Arrastar = mover, roda = zoom, botao direito = menu de marcacoes,
// clique esquerdo numa marcacao = editar. M ou Esc = fechar.
public class WorldMapScreen extends Screen {
    private record Mark(int x, int z, String label) {}
    private record MenuItem(String label, Runnable action) {}

    private int px, py, pw, ph;            // janela
    private int mapX, mapY, mapW, mapH;    // area do mapa
    private double cx, cz;
    private double zoom = 1.0;
    private final List<Mark> marks = new ArrayList<>();
    private long lastMarkScan = -1000;
    private boolean mode3D;
    private boolean rDown, rMoved;
    private double rStartX, rStartY;

    // menu de contexto
    private final List<MenuItem> menu = new ArrayList<>();
    private int menuX, menuY, menuW;

    public WorldMapScreen() {
        super(Component.literal("Mapa-Mundi"));
        MapOptions.ensureLoaded();
        mode3D = true;   // o mapa-mundi e' sempre 3D
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) { cx = p.getX(); cz = p.getZ(); }
    }

    @Override
    protected void init() {
        pw = Math.max(300, Math.min(width - 24, (int) (width * 0.70)));
        ph = Math.max(200, Math.min(height - 24, (int) (height * 0.76)));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        mapX = px + 4; mapY = py + 24; mapW = pw - 8; mapH = ph - 24 - 18;

        int bx = px + pw - 22;
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose()).bounds(bx, py + 3, 18, 16).build());
        bx -= 72;
        addRenderableWidget(Button.builder(Component.literal("Centralizar"), b -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) { cx = p.getX(); cz = p.getZ(); }
        }).bounds(bx, py + 3, 70, 16).build());
        bx -= 54;
        addRenderableWidget(Button.builder(Component.literal("Opções"), b ->
                Minecraft.getInstance().setScreen(new MapOptionsScreen(this))).bounds(bx, py + 3, 52, 16).build());
        bx -= 74;
        addRenderableWidget(Button.builder(Component.literal("Marcações"), b ->
                Minecraft.getInstance().setScreen(new WaypointListScreen(this))).bounds(bx, py + 3, 72, 16).build());
    }

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

        g.fill(0, 0, width, height, 0x80000000);   // escurece o jogo atrás da janela
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF000000);
        g.fill(px, py, px + pw, py + ph, 0xFF2A2A38);
        g.drawString(font, "Mapa-Mundi", px + 8, py + 8, 0xFFFFFFFF, true);
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 0xFF000000);
        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0A0A0F);

        LocalPlayer pl = mc.player;
        if (mode3D && DioramaRenderer.ready()) {
            WorldMap3D.render(g, mapX, mapY, mapW, mapH, cx, cz, pl);
        } else if (mode3D) {
            g.drawCenteredString(font, "Shader do mapa indisponível", mapX + mapW / 2, mapY + mapH / 2, 0xFFFF8888);
        }
        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        if (!mode3D) {
            drawTerrain(g);
            if (MapOptions.chunkGrid && zoom >= 1.5) drawGrid(g);
        }

        MapWaypoints.Waypoint hover = waypointAt(mx, my);
        if (MapOptions.worldPins) {
            for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
                if (w.hidden) continue;
                float[] s = screenOf(w.x + 0.5, w.y, w.z + 0.5);
                if (s[2] == 0f) continue;
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 6, sy - 6, sx + 7, sy + 7, w == hover ? 0xFFFFFFFF : 0xFF000000);
                g.fill(sx - 5, sy - 5, sx + 6, sy + 6, w.color);
                WaypointIcons.draw(g, w.icon(), sx - 5, sy - 5, 11);
                if (MapOptions.worldLabels) {
                    String label = w.name;
                    if (MapOptions.showDistance && pl != null)
                        label += " (" + (int) Math.sqrt(pl.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + "m)";
                    g.drawString(font, label, sx + 9, sy - 4, 0xFFFFFFFF, true);
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
        if (pl != null && !mode3D) {
            g.pose().pushPose();
            g.pose().translate(toScreenX(pl.getX()), toScreenY(pl.getZ()), 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(pl.getYRot() - 180f));
            g.fill(-1, -5, 2, 4, 0xFF000000);
            g.fill(-3, -2, 4, 0, 0xFF000000);
            g.fill(0, -4, 1, 3, 0xFFFFFFFF);
            g.fill(-2, -2, 3, -1, 0xFFFFFFFF);
            g.pose().popPose();
        }
        g.disableScissor();

        int[] under = inMap(mx, my) ? pickWorld(mx, my) : null;
        String info = "";
        if (under != null) {
            int h = ExploredMap.heightAt(under[0], under[1]);
            info = "X: " + under[0] + "  Z: " + under[1] + (h == ExploredMap.UNKNOWN ? "" : "  Y: " + h);
        }
        g.drawString(font, info, mapX + 2, mapY + mapH + 5, 0xFFFFFFFF, false);
        String hint = mode3D ? "Arraste: mover | Botão dir. arrastar: girar | Roda: zoom | Botão dir.: menu"
                : "Botão direito: menu  |  Roda: zoom  |  Arraste: mover";
        g.drawString(font, hint, mapX + mapW - font.width(hint) - 2, mapY + mapH + 5, 0xFF9A9AAA, false);

        super.render(g, mx, my, pt);

        if (hover != null && menu.isEmpty()) {
            g.renderTooltip(font, Component.literal(hover.name + " (" + hover.x + ", " + hover.y + ", " + hover.z + ")  [" + hover.setName() + "]"), mx, my);
        }
        drawMenu(g, mx, my);
    }

    private void drawTerrain(GuiGraphics g) {
        int minCx = (int) Math.floor(toWorldX(mapX) / 16) - 1, maxCx = (int) Math.floor(toWorldX(mapX + mapW) / 16) + 1;
        int minCz = (int) Math.floor(toWorldZ(mapY) / 16) - 1, maxCz = (int) Math.floor(toWorldZ(mapY + mapH) / 16) + 1;
        for (int tz = minCz; tz <= maxCz; tz++) {
            for (int tx = minCx; tx <= maxCx; tx++) {
                ExploredMap.Tile t = ExploredMap.tile(tx, tz);
                if (t == null) continue;
                ResourceLocation tex = ExploredMap.texture(t);
                g.pose().pushPose();
                g.pose().translate(toScreenX(tx * 16.0), toScreenY(tz * 16.0), 0);
                g.pose().scale((float) zoom, (float) zoom, 1f);
                g.blit(tex, 0, 0, 16, 16, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
            }
        }
    }

    private void drawGrid(GuiGraphics g) {
        int minCx = (int) Math.floor(toWorldX(mapX) / 16), maxCx = (int) Math.floor(toWorldX(mapX + mapW) / 16) + 1;
        int minCz = (int) Math.floor(toWorldZ(mapY) / 16), maxCz = (int) Math.floor(toWorldZ(mapY + mapH) / 16) + 1;
        for (int c = minCx; c <= maxCx; c++) { int x = (int) toScreenX(c * 16.0); g.fill(x, mapY, x + 1, mapY + mapH, 0x30FFFFFF); }
        for (int c = minCz; c <= maxCz; c++) { int y = (int) toScreenY(c * 16.0); g.fill(mapX, y, mapX + mapW, y + 1, 0x30FFFFFF); }
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
            menu.add(new MenuItem("Teleportar", () -> teleport(w.x, w.y, w.z)));
        } else {
            menu.add(new MenuItem("Criar marcação aqui...", () -> mc.setScreen(new WaypointEditScreen(this, null, wx, wy, wz))));
            menu.add(new MenuItem("Marcação rápida aqui", () -> MapWaypoints.add(wx, wy, wz)));
            menu.add(new MenuItem("Copiar coordenadas", () -> mc.keyboardHandler.setClipboard(wx + " " + wy + " " + wz)));
            menu.add(new MenuItem("Centralizar aqui", () -> { cx = wx; cz = wz; }));
            menu.add(new MenuItem("Teleportar", () -> teleport(wx, wy, wz)));
        }
        int widest = 0;
        for (MenuItem it : menu) widest = Math.max(widest, font.width(it.label()));
        menuW = widest + 12;
        menuX = (int) Math.min(mx, width - menuW - 2);
        menuY = (int) Math.min(my, height - menu.size() * 14 - 2);
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
        g.fill(menuX - 1, menuY - 1, menuX + menuW + 1, menuY + menu.size() * 14 + 1, 0xFF000000);
        for (int i = 0; i < menu.size(); i++) {
            int y = menuY + i * 14;
            boolean over = mx >= menuX && mx < menuX + menuW && my >= y && my < y + 14;
            g.fill(menuX, y, menuX + menuW, y + 14, over ? 0xFF6B6B8A : 0xFF2A2A38);
            g.drawString(font, menu.get(i).label(), menuX + 6, y + 3, 0xFFFFFFFF, false);
        }
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!menu.isEmpty()) {
            for (int i = 0; i < menu.size(); i++) {
                int y = menuY + i * 14;
                if (mx >= menuX && mx < menuX + menuW && my >= y && my < y + 14) {
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
