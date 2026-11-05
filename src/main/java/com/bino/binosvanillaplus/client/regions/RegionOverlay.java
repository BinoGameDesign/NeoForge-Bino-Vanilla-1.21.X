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
    // losango colorido da dificuldade (faixa de nivel dos mobs)
    public static void diamond(GuiGraphics g, float cx, float cy, float size, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45f));
        float h = size / 2f;
        g.pose().scale(h, h, 1f);
        g.fill(-1, -1, 1, 1, argb);
        g.pose().popPose();
    }

    // bioma da regiao (guardado como texto) -> Holder do registro de biomas do cliente
    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome(String id) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || id == null) return null;
        var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return mc.level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, rl)).orElse(null);
    }

    // devolve o texto do hover ("Mobs: Nv. 21-25") se o mouse estiver em cima de um losango
    public static String labels(GuiGraphics g, Font font, Projector pr, double ox, double oz, double radius, double fallbackY,
                              int mapX, int mapY, int mapW, int mapH, int mx, int my) {
        String hover = null;
        var mcl = net.minecraft.client.Minecraft.getInstance().level;
        double r2 = radius * radius;
        List<int[]> taken = new ArrayList<>();
        int shown = 0;
        for (BiomeRegions.Label l : BiomeRegions.labels()) {
            if (shown >= 40) break;
            double dx = l.x() - ox, dz = l.z() - oz;
            if (dx * dx + dz * dz > r2) continue;
            float[] s = pr.project(l.x(), surfaceY(l.x(), l.z(), fallbackY) + 3, l.z());
            if (s[2] == 0f) continue;
            float k = 0.7f;
            int w = (int) (font.width(l.name()) * k) + 16, h = 9;
            int x = (int) s[0] - w / 2, y = (int) s[1] - h / 2;
            if (x < mapX || y < mapY || x + w > mapX + mapW || y + h > mapY + mapH) continue;
            boolean clash = false;
            for (int[] t : taken) {
                if (x < t[0] + t[2] && x + w > t[0] && y < t[1] + t[3] && y + h > t[1]) { clash = true; break; }
            }
            if (clash) continue;
            taken.add(new int[]{x, y, w, h});
            g.fill(x, y, x + w, y + h, 0x70000000);
            g.pose().pushPose();
            g.pose().translate(x + 13, y + 1.5f, 0);
            g.pose().scale(k, k, 1f);
            g.drawString(font, l.name(), 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
            if (mcl != null) {
                double lv = com.bino.binosvanillaplus.leveling.RegionDifficulty.regionLevel(mcl, l.x(), l.z(), biome(l.biome()));
                if (lv >= 0) {
                    diamond(g, x + 6, y + 4.5f, 7, com.bino.binosvanillaplus.client.leveling.LevelClient.bandColor(lv));
                    if (mx >= x && mx < x + 12 && my >= y - 1 && my < y + h + 1)
                        hover = "Mobs: " + com.bino.binosvanillaplus.leveling.RegionDifficulty.bandText(lv);
                }
            }
            shown++;
        }
        return hover;
    }
}
