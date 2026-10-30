package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

// Mapas em molduras com RELEVO (estilo Maps3D). A altura vem do que voce ja explorou (ExploredMap);
// onde nao ha dados, o mapa fica plano como o vanilla. A malha e' guardada em cache por mapa.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class FrameReliefEvents {
    private static final int CELLS = 64;                 // 64x64 celulas de 2x2 pixels do mapa
    private static final float SEA = 63f;
    private static final float EXAGGERATION = 1.6f;
    private static final float MAX_DIST_SQ = 24f * 24f;

    private static class Mesh {
        long builtAt;
        float[] top = new float[CELLS * CELLS];          // altura (px) de cada celula
        int[] color = new int[CELLS * CELLS];            // ARGB
        boolean[] known = new boolean[CELLS * CELLS];
    }
    private static final Map<String, Mesh> CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onFrame(RenderItemInFrameEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FILLED_MAP)) return;
        Minecraft mc = Minecraft.getInstance();
        ItemFrame frame = event.getItemFrameEntity();
        if (mc.level == null || mc.player == null || frame.distanceToSqr(mc.player) > MAX_DIST_SQ) return;
        MapItemSavedData data = MapItem.getSavedData(stack, mc.level);
        if (data == null || data.dimension != mc.level.dimension()) return;

        String id = data.centerX + "," + data.centerZ + "," + data.scale + "," + data.dimension.location();
        Mesh mesh = CACHE.get(id);
        long now = mc.level.getGameTime();
        if (mesh == null || now - mesh.builtAt > 200) {
            mesh = build(data, now);
            CACHE.put(id, mesh);
            if (CACHE.size() > 32) CACHE.clear();
        }
        // sem nenhuma coluna conhecida: deixa o desenho vanilla
        boolean any = false;
        for (boolean k : mesh.known) if (k) { any = true; break; }
        if (!any) return;

        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // mesmas transformacoes do vanilla para mapas: espaco em "pixels do mapa" (0..128), z negativo = para o jogador
        pose.mulPose(Axis.ZP.rotationDegrees(180f));
        float s = 0.0078125f;
        pose.scale(s, s, s);
        pose.translate(-64f, -64f, 0f);
        pose.translate(0f, 0f, -1f);

        int packed = event.getPackedLight();
        float light = Math.max(LightTexture.sky(packed), LightTexture.block(packed)) / 15f * 0.75f + 0.25f;
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        draw(vc, m, mesh, light);
        pose.popPose();
    }

    private static Mesh build(MapItemSavedData data, long now) {
        Mesh mesh = new Mesh();
        mesh.builtAt = now;
        int blocksPerPx = 1 << data.scale;
        float k = EXAGGERATION / blocksPerPx;
        for (int cz = 0; cz < CELLS; cz++) {
            for (int cx = 0; cx < CELLS; cx++) {
                int px = cx * 2, pz = cz * 2, i = cz * CELLS + cx;
                int packed = data.colors[pz * 128 + px] & 0xFF;
                int abgr = packed == 0 ? 0xFF202020 : MapColor.getColorFromPackedId(packed);
                mesh.color[i] = 0xFF000000 | (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                int wx = data.centerX + (px + 1 - 64) * blocksPerPx, wz = data.centerZ + (pz + 1 - 64) * blocksPerPx;
                int h = ExploredMap.heightAt(wx, wz);
                if (h == ExploredMap.UNKNOWN) { mesh.top[i] = 0f; continue; }
                mesh.known[i] = true;
                mesh.top[i] = Math.max(-10f, Math.min(34f, (h - SEA) * k));
            }
        }
        return mesh;
    }

    private static void draw(VertexConsumer vc, Matrix4f m, Mesh mesh, float light) {
        final float base = -6f;      // fundo da "placa" (px, para tras da moldura)
        for (int cz = 0; cz < CELLS; cz++) {
            for (int cx = 0; cx < CELLS; cx++) {
                int i = cz * CELLS + cx;
                float x0 = cx * 2f, x1 = x0 + 2f, y0 = cz * 2f, y1 = y0 + 2f;
                float z = -mesh.top[i];               // negativo = mais perto de quem olha
                int col = lit(mesh.color[i], light);
                quad(vc, m, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, col);

                int side = lit(mesh.color[i], light * 0.7f);
                // laterais para as 4 vizinhas mais baixas (ou para a borda do mapa)
                if (cx == 0 || mesh.top[i - 1] < mesh.top[i])
                    quad(vc, m, x0, y0, z, x0, y1, z, x0, y1, zOf(mesh, cx - 1, cz, base), x0, y0, zOf(mesh, cx - 1, cz, base), side);
                if (cx == CELLS - 1 || mesh.top[i + 1] < mesh.top[i])
                    quad(vc, m, x1, y0, z, x1, y1, z, x1, y1, zOf(mesh, cx + 1, cz, base), x1, y0, zOf(mesh, cx + 1, cz, base), side);
                if (cz == 0 || mesh.top[i - CELLS] < mesh.top[i])
                    quad(vc, m, x0, y0, z, x1, y0, z, x1, y0, zOf(mesh, cx, cz - 1, base), x0, y0, zOf(mesh, cx, cz - 1, base), side);
                if (cz == CELLS - 1 || mesh.top[i + CELLS] < mesh.top[i])
                    quad(vc, m, x0, y1, z, x1, y1, z, x1, y1, zOf(mesh, cx, cz + 1, base), x0, y1, zOf(mesh, cx, cz + 1, base), side);
            }
        }
    }

    private static float zOf(Mesh mesh, int cx, int cz, float base) {
        if (cx < 0 || cz < 0 || cx >= CELLS || cz >= CELLS) return -base;     // borda: desce ate o fundo
        return -mesh.top[cz * CELLS + cx];
    }

    private static int lit(int argb, float f) {
        int r = (int) (((argb >> 16) & 0xFF) * f), g = (int) (((argb >> 8) & 0xFF) * f), b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // cada quadrado e' emitido nas duas ordens de vertices (frente e verso), assim nao depende do culling
    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int col) {
        vc.addVertex(m, ax, ay, az).setColor(col);
        vc.addVertex(m, bx, by, bz).setColor(col);
        vc.addVertex(m, cx, cy, cz).setColor(col);
        vc.addVertex(m, dx, dy, dz).setColor(col);
        vc.addVertex(m, dx, dy, dz).setColor(col);
        vc.addVertex(m, cx, cy, cz).setColor(col);
        vc.addVertex(m, bx, by, bz).setColor(col);
        vc.addVertex(m, ax, ay, az).setColor(col);
    }
}
