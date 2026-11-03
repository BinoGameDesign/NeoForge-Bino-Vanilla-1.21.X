package com.bino.binosvanillaplus.client.regions;

import com.bino.binosvanillaplus.client.map.ExploredMap;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

// Desenho dos contornos brancos e dos nomes das regioes sobre o mapa-mundi e o minimapa.
public final class RegionOverlay {
    private RegionOverlay() {}

    public interface Projector { float[] project(double wx, double wy, double wz); }   // {sx, sy, visivel}

    // linha de espessura 't' entre dois pontos da tela
    public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, int argb, float t) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.pose().translate(0, -t / 2f, 0);
        g.pose().scale(1f, t, 1f);
        g.fill(0, 0, Math.max(1, (int) Math.ceil(len)), 1, argb);
        g.pose().popPose();
    }

    private static double surfaceY(int x, int z, double fallback) {
        int h = ExploredMap.heightAt(x, z);
        return (h == ExploredMap.UNKNOWN ? fallback : h) + 1.0;
    }

    // contornos em volta de (ox, oz) no raio dado
    public static void edges(GuiGraphics g, Projector pr, double ox, double oz, double radius, double fallbackY,
                             float thickness, int argb, int maxSegments) {
        int[] e = BiomeRegions.edges();
        double r2 = radius * radius;
        int drawn = 0;
        for (int i = 0; i + 3 < e.length && drawn < maxSegments; i += 4) {
            double mx = (e[i] + e[i + 2]) * 0.5 - ox, mz = (e[i + 1] + e[i + 3]) * 0.5 - oz;
            if (mx * mx + mz * mz > r2) continue;
            float[] a = pr.project(e[i], surfaceY(e[i], e[i + 1], fallbackY), e[i + 1]);
            float[] b = pr.project(e[i + 2], surfaceY(e[i + 2], e[i + 3], fallbackY), e[i + 3]);
            if (a[2] == 0f || b[2] == 0f) continue;
            line(g, a[0], a[1], b[0], b[1], argb, thickness);
            drawn++;
        }
    }

    // nomes das regioes (as maiores primeiro; um nome que bateria em outro ja desenhado e pulado)
    public static void labels(GuiGraphics g, Font font, Projector pr, double ox, double oz, double radius, double fallbackY,
                              int mapX, int mapY, int mapW, int mapH) {
        double r2 = radius * radius;
        List<int[]> taken = new ArrayList<>();
        int shown = 0;
        for (BiomeRegions.Label l : BiomeRegions.labels()) {
            if (shown >= 40) break;
            double dx = l.x() - ox, dz = l.z() - oz;
            if (dx * dx + dz * dz > r2) continue;
            float[] s = pr.project(l.x(), surfaceY(l.x(), l.z(), fallbackY) + 3, l.z());
            if (s[2] == 0f) continue;
            int w = font.width(l.name()) + 8, h = 12;
            int x = (int) s[0] - w / 2, y = (int) s[1] - h / 2;
            if (x < mapX || y < mapY || x + w > mapX + mapW || y + h > mapY + mapH) continue;
            boolean clash = false;
            for (int[] t : taken) {
                if (x < t[0] + t[2] && x + w > t[0] && y < t[1] + t[3] && y + h > t[1]) { clash = true; break; }
            }
            if (clash) continue;
            taken.add(new int[]{x, y, w, h});
            g.fill(x, y, x + w, y + h, 0x70000000);
            g.drawString(font, l.name(), x + 4, y + 2, 0xFFFFFFFF, true);
            shown++;
        }
    }
}
