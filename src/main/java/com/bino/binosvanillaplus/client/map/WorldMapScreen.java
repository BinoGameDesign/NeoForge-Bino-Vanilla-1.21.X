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
import java.util.Map;

// Mapa-mundi: arrastar = mover, roda = zoom, botao direito = criar marcacao (ou apagar se clicar numa),
// M ou Esc = fechar. Mostra o X dos mapas do tesouro que estiverem no inventario.
public class WorldMapScreen extends Screen {
    private record Mark(int x, int z, String label) {}

    private double cx, cz;
    private double zoom = 2.0;
    private final List<Mark> marks = new ArrayList<>();
    private long lastMarkScan = -1000;

    public WorldMapScreen() {
        super(Component.translatableWithFallback("map.binosvanillaplus.title", "Mapa-Mundi"));
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) { cx = p.getX(); cz = p.getZ(); }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatableWithFallback("map.binosvanillaplus.center", "Centralizar"), b -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) { cx = p.getX(); cz = p.getZ(); }
        }).bounds(width - 88, height - 24, 80, 18).build());
    }

    private double toWorldX(double sx) { return cx + (sx - width / 2.0) / zoom; }
    private double toWorldZ(double sy) { return cz + (sy - height / 2.0) / zoom; }
    private float toScreenX(double wx) { return (float) (width / 2.0 + (wx - cx) * zoom); }
    private float toScreenY(double wz) { return (float) (height / 2.0 + (wz - cz) * zoom); }

    // marcas de mapas do tesouro/exploracao que o jogador carrega
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
                else if (d.type().is(ResourceLocation.withDefaultNamespace("woodland_mansion"))) label = "Mans\u00e3o";
                else if (d.type().is(ResourceLocation.withDefaultNamespace("monument"))) label = "Monumento";
                if (label == null) continue;
                int scale = 1 << data.scale;
                int wx = data.centerX + (int) (d.x() / 2.0 * scale);
                int wz = data.centerZ + (int) (d.y() / 2.0 * scale);
                marks.add(new Mark(wx, wz, label));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getGameTime() - lastMarkScan > 20) { scanMarks(); lastMarkScan = mc.level.getGameTime(); }

        g.fill(0, 0, width, height, 0xFF0A0A0F);

        // terreno
        int minCx = (int) Math.floor(toWorldX(0) / 16) - 1, maxCx = (int) Math.floor(toWorldX(width) / 16) + 1;
        int minCz = (int) Math.floor(toWorldZ(0) / 16) - 1, maxCz = (int) Math.floor(toWorldZ(height) / 16) + 1;
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

        // marcacoes do jogador
        MapWaypoints.Waypoint hoverWp = null;
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            int sx = (int) toScreenX(w.x + 0.5), sy = (int) toScreenY(w.z + 0.5);
            g.fill(sx - 4, sy - 4, sx + 5, sy + 5, 0xFF000000);
            g.fill(sx - 3, sy - 3, sx + 4, sy + 4, w.color);
            g.drawString(font, w.name, sx + 7, sy - 4, 0xFFFFFFFF, true);
            if (Math.abs(mx - sx) <= 5 && Math.abs(my - sy) <= 5) hoverWp = w;
        }
        // tesouros / estruturas de mapas
        for (Mark m : marks) {
            int sx = (int) toScreenX(m.x() + 0.5), sy = (int) toScreenY(m.z() + 0.5);
            g.drawCenteredString(font, "X", sx, sy - 4, 0xFFFF2222);
            g.drawString(font, m.label(), sx + 7, sy - 4, 0xFFFFC0C0, true);
        }
        // jogador
        LocalPlayer p = mc.player;
        if (p != null) {
            g.pose().pushPose();
            g.pose().translate(toScreenX(p.getX()), toScreenY(p.getZ()), 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(p.getYRot() - 180f));
            g.fill(-1, -5, 2, 4, 0xFF000000);
            g.fill(-3, -2, 4, 0, 0xFF000000);
            g.fill(0, -4, 1, 3, 0xFFFFFFFF);
            g.fill(-2, -2, 3, -1, 0xFFFFFFFF);
            g.pose().popPose();
        }

        // rodape
        int wx = (int) Math.floor(toWorldX(mx)), wz = (int) Math.floor(toWorldZ(my));
        int h = ExploredMap.heightAt(wx, wz);
        String info = "X: " + wx + "  Z: " + wz + (h == ExploredMap.UNKNOWN ? "" : "  Y: " + h);
        g.drawString(font, info, 6, height - 12, 0xFFFFFFFF, true);
        g.drawString(font, Component.translatableWithFallback("map.binosvanillaplus.help",
                "Arraste: mover  |  Roda: zoom  |  Bot\u00e3o direito: criar/apagar marca\u00e7\u00e3o"), 6, 6, 0xFFBBBBBB, true);

        super.render(g, mx, my, pt);
        if (hoverWp != null) g.renderTooltip(font, Component.literal(hoverWp.name + " (" + hoverWp.x + ", " + hoverWp.y + ", " + hoverWp.z + ")"), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 1) {
            for (MapWaypoints.Waypoint w : new ArrayList<>(MapWaypoints.list())) {
                if (Math.abs(mx - toScreenX(w.x + 0.5)) <= 5 && Math.abs(my - toScreenY(w.z + 0.5)) <= 5) {
                    MapWaypoints.remove(w);
                    return true;
                }
            }
            int wx = (int) Math.floor(toWorldX(mx)), wz = (int) Math.floor(toWorldZ(my));
            int h = ExploredMap.heightAt(wx, wz);
            MapWaypoints.add(wx, h == ExploredMap.UNKNOWN ? 64 : h + 1, wz);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            cx -= dx / zoom;
            cz -= dy / zoom;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) {
            zoom = Math.max(0.25, Math.min(10.0, zoom * (sy > 0 ? 1.25 : 0.8)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (MapClientEvents.OPEN_MAP.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // Sem isto o Screen.render do vanilla aplica o BLUR do fundo por cima de tudo que ja foi desenhado (era o que deixava a tela borrada)
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
