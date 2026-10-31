package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.client.ui.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Mapa-mundi (Figma "World Map"): o mapa 3D e' a propria janela.
// UX: zoom e "centralizar" suaves, menu de contexto animado (botao direito), painel de ajuda que sobe com fade,
// marcacao sob o mouse cresce, avisos ("Coordenadas copiadas"). Arrastar = mover, botao direito arrastar = girar,
// roda = zoom, clique numa marcacao = editar, M ou Esc = fechar. Atalhos: C centraliza, H ajuda.
public class WorldMapScreen extends BinoScreen {
    private record Mark(int x, int z, String label) {}

    private int mapX, mapY, mapW, mapH;
    private double cx, cz;
    private final List<Mark> marks = new ArrayList<>();
    private long lastMarkScan = -1000;
    private boolean rDown, rMoved;
    private double rStartX, rStartY;
    private boolean help = false;
    private final PopupMenu menu = new PopupMenu("Menu");
    private final Tween helpT = new Tween(0f, 55f);
    private float distTarget = WorldMap3D.dist;
    private final Tween centerX = new Tween(0f, 70f), centerZ = new Tween(0f, 70f);
    private boolean centering = false;
    private MapWaypoints.Waypoint lastHover;
    private final Tween hoverGrow = new Tween(0f, 40f);

    private static final String[] HELP = {
            "Para mover a tela: arraste com o botão esquerdo para os cantos.",
            "Para girar a tela: arraste com o botão direito para os cantos.",
            "Para dar zoom: suba ou desça a roda do mouse.",
            "Para acessar o menu: aperte o botão direito."};

    public WorldMapScreen() {
        super("World Map", null);
        MapOptions.ensureLoaded();
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) { cx = p.getX(); cz = p.getZ(); }
    }

    @Override
    protected int[] panelBounds() { return new int[]{mapX, mapY, mapW, mapH}; }

    @Override
    protected boolean animateScale() { return false; }   // o mapa 3D usa viewport proprio

    @Override
    protected void build() {
        mapW = Math.max(280, Math.min(width - 24, (int) (width * 0.735)));
        mapH = Math.max(180, Math.min(height - 34, (int) (height * 0.77)));
        mapX = (width - mapW) / 2;
        mapY = (height - mapH) / 2 + 4;

        addRenderableWidget(BinoUi.closeButton(mapX + mapW - 8, mapY - 6, 13, this::onClose));
        int by = mapY + mapH - 12, bx = mapX + mapW - 10 - 4 * 24 + 5;
        addRenderableWidget(new BinoUi.Button(bx, by, 19, 19, "", BinoUi.Style.LIGHT,
                () -> Minecraft.getInstance().setScreen(new WaypointListScreen(this))).icon(BinoUi.tex("map/waypoints"), 11).tooltip("Marcações"));
        addRenderableWidget(new BinoUi.Button(bx + 24, by, 19, 19, "", BinoUi.Style.LIGHT,
                () -> Minecraft.getInstance().setScreen(new MapOptionsScreen(this))).icon(BinoUi.tex("map/options"), 11).tooltip("Opções"));
        addRenderableWidget(new BinoUi.Button(bx + 48, by, 19, 19, "", BinoUi.Style.LIGHT, this::centerOnPlayer)
                .icon(BinoUi.tex("map/center"), 11).tooltip("Centralizar no jogador (C)"));
        addRenderableWidget(new BinoUi.Button(bx + 72, by, 19, 19, "?", BinoUi.Style.LIGHT, () -> help = !help).tooltip("Ajuda (H)"));
    }

    private void centerOnPlayer() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) animateTo(p.getX(), p.getZ());
    }

    // desliza a camera ate (x, z)
    private void animateTo(double x, double z) {
        centerX.snap((float) cx).to((float) x);
        centerZ.snap((float) cz).to((float) z);
        centering = true;
    }

    private int[] helpRect() {
        int hw = Math.min(266, mapW - 24), hh = 104;
        return new int[]{mapX + mapW - hw - 10, mapY + mapH - hh - 22, hw, hh};
    }

    // ---------------------------------------------------------------- coordenadas

    // posicao na tela de um ponto do mundo; [2] == 0 -> atras da camera
    private float[] screenOf(double wx, double wy, double wz) {
        return DioramaRenderer.projectWith(WorldMap3D.view, WorldMap3D.proj, (float) (wx - cx), (float) (wy - WorldMap3D.targetY),
                (float) (wz - cz), mapX, mapY, mapW, mapH);
    }

    private int[] pickWorld(double mx, double my) { return WorldMap3D.pick(mx, my, mapX, mapY, mapW, mapH, cx, cz); }

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
            if (s[2] != 0f && Math.abs(mx - s[0]) <= 7 && Math.abs(my - s[1]) <= 7) return w;
        }
        return null;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getGameTime() - lastMarkScan > 20) { scanMarks(); lastMarkScan = mc.level.getGameTime(); }
        // animacoes da camera
        WorldMap3D.dist += (distTarget - WorldMap3D.dist) * 0.25f;
        if (centering) {
            cx = centerX.get(); cz = centerZ.get();
            if (Math.abs(centerX.target() - cx) < 0.05 && Math.abs(centerZ.target() - cz) < 0.05) centering = false;
        }

        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0B0F14);
        LocalPlayer pl = mc.player;
        if (DioramaRenderer.ready()) WorldMap3D.render(g, mapX, mapY, mapW, mapH, cx, cz, pl);
        else g.drawCenteredString(font, "Shader do mapa indisponível", mapX + mapW / 2, mapY + mapH / 2, 0xFFFF8888);

        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        boolean free = !menu.isOpen() && !help;
        MapWaypoints.Waypoint hover = free ? waypointAt(mx, my) : null;
        if (hover != lastHover) { hoverGrow.snap(0f); lastHover = hover; }
        float grow = hoverGrow.get(hover != null ? 1f : 0f);
        if (MapOptions.worldPins) {
            for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
                if (w.hidden) continue;
                float[] s = screenOf(w.x + 0.5, w.y, w.z + 0.5);
                if (s[2] == 0f) continue;
                int sx = (int) s[0], sy = (int) s[1];
                float k = w == hover ? 1f + 0.35f * Ease.outBack(grow) : 1f;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(k, k, 1f);
                g.fill(-7, -7, 8, 8, w == hover ? 0xFFFFFFFF : 0xFF000000);
                WaypointIcons.badge(g, w.icon(), -6, -6, 13, w.color);
                g.pose().popPose();
                if (MapOptions.worldLabels) {
                    String label = w.name;
                    if (MapOptions.showDistance && pl != null)
                        label += " (" + (int) Math.sqrt(pl.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5)) + " m)";
                    int lx = sx + (int) (9 * k);
                    g.fill(lx, sy - 6, lx + 4 + font.width(label), sy + 6, w == hover ? 0xB0000000 : 0x80000000);
                    g.drawString(font, label, lx + 2, sy - 4, 0xFFFFFFFF, false);
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
        if (hover != null) {
            setTooltipForNextRenderPass(Component.literal(hover.name + " (" + hover.x + ", " + hover.y + ", " + hover.z + ")  [" + hover.setName() + "]"));
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, "World Map", mapX + mapW / 2, mapY - 7);
        drawHelp(g);
        menu.render(g, mx, my);
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

    // painel de ajuda: sobe 8 px e aparece com fade
    private void drawHelp(GuiGraphics g) {
        float v = helpT.get(help ? 1f : 0f);
        if (v < 0.01f) return;
        int[] r = helpRect();
        g.pose().pushPose();
        g.pose().translate(0, (1f - Ease.outCubic(v)) * 8f, 200);
        g.flush();
        float[] prev = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3] * v);
        g.fill(r[0] + 2, r[1] + 2, r[0] + r[2] + 2, r[1] + r[3] + 2, 0x40000000);
        BinoUi.round(g, r[0], r[1], r[2], r[3], BinoUi.MENU_BG);
        BinoUi.titleLeft(g, font, "Help", r[0] + 12, r[1] - 7);
        int y = r[1] + 14;
        for (String line : HELP) {
            for (FormattedCharSequence seq : font.split(Component.literal(line), r[2] - 18)) {
                g.drawString(font, seq, r[0] + 8, y, 0xFFFFFFFF, false);
                y += 10;
            }
            y += 4;
        }
        g.flush();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- menu de contexto

    private void openMenu(double mx, double my) {
        MapWaypoints.Waypoint w = waypointAt(mx, my);
        int[] at = pickWorld(mx, my);
        if (w == null && at == null) return;
        int wx = at == null ? w.x : at[0], wz = at == null ? w.z : at[1];
        int h = ExploredMap.heightAt(wx, wz);
        int wy = h == ExploredMap.UNKNOWN ? 64 : h + 1;
        Minecraft mc = Minecraft.getInstance();
        List<PopupMenu.Item> items = new ArrayList<>();
        if (w != null) {
            items.add(PopupMenu.Item.of("Editar marcação", () -> mc.setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z))));
            items.add(PopupMenu.Item.of(w.hidden ? "Mostrar" : "Ocultar", () -> { w.hidden = !w.hidden; MapWaypoints.save(); }));
            items.add(PopupMenu.Item.of("Excluir", () -> {
                MapWaypoints.remove(w);
                Toasts.show("\"" + w.name + "\" excluída", "Desfazer", () -> MapWaypoints.add(w));
            }));
            items.add(PopupMenu.Item.of("Copiar coordenadas", () -> copy(w.x, w.y, w.z)));
            items.add(PopupMenu.Item.of("Centralizar aqui", () -> animateTo(w.x + 0.5, w.z + 0.5)));
            if (canTeleport()) items.add(PopupMenu.Item.of("Teleportar", () -> teleport(w.x, w.y, w.z)));
        } else {
            items.add(PopupMenu.Item.of("Criar marcação aqui", () -> mc.setScreen(new WaypointEditScreen(this, null, wx, wy, wz))));
            items.add(PopupMenu.Item.of("Marcação rápida aqui", () -> {
                MapWaypoints.Waypoint nw = MapWaypoints.create(wx, wy, wz);
                MapWaypoints.add(nw);
                Toasts.show("\"" + nw.name + "\" criada", "Desfazer", () -> MapWaypoints.remove(nw));
            }));
            items.add(PopupMenu.Item.of("Copiar coordenadas", () -> copy(wx, wy, wz)));
            items.add(PopupMenu.Item.of("Centralizar aqui", () -> animateTo(wx + 0.5, wz + 0.5)));
            if (canTeleport()) items.add(PopupMenu.Item.of("Teleportar", () -> teleport(wx, wy, wz)));
        }
        menu.open((int) mx, (int) my, items, 0, width, height);
    }

    private void copy(int x, int y, int z) {
        Minecraft.getInstance().keyboardHandler.setClipboard(x + " " + y + " " + z);
        Toasts.show("Coordenadas copiadas: " + x + " " + y + " " + z);
    }

    // teleporte so aparece no modo criativo
    private static boolean canTeleport() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.isCreative();
    }

    private void teleport(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand("tp @s " + x + " " + y + " " + z);
        closeTo(null);
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isClosing()) return true;
        if (menu.mouseClicked(mx, my)) return true;
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
                UiSound.click();
                Minecraft.getInstance().setScreen(new WaypointEditScreen(this, w, w.x, w.y, w.z));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (menu.isOpen()) return true;
        if (button == 0) {
            centering = false;
            double k = WorldMap3D.dist / Math.max(1, mapH) * 0.93;
            double sy = Math.sin(WorldMap3D.yaw), cyw = Math.cos(WorldMap3D.yaw);
            cx += (sy * dy - cyw * dx) * k;      // frente = (sin, -cos), direita = (cos, sin)
            cz += (-cyw * dy - sy * dx) * k;
            return true;
        }
        if (button == 1 && rDown) {
            if (Math.abs(mx - rStartX) + Math.abs(my - rStartY) > 4) rMoved = true;
            if (rMoved) {
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
        if (menu.isOpen()) return true;
        if (sy != 0) {
            distTarget = (float) Math.max(18, Math.min(700, distTarget * (sy > 0 ? 0.82 : 1.22)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (menu.keyPressed(key)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE && help) { help = false; return true; }
        if (MapClientEvents.OPEN_MAP.matches(key, scan)) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_C) { centerOnPlayer(); return true; }
        if (key == GLFW.GLFW_KEY_H) { help = !help; return true; }
        return super.keyPressed(key, scan, mods);
    }
}
