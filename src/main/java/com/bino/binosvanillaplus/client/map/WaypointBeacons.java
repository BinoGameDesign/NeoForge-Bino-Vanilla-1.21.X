package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// Feixe de luz das marcacoes no mundo, na camera normal do jogador: usa o proprio renderizador de beacon do jogo
// (textura animada, giro), com a cor da marcacao. Aparece quando o jogador tem um mapa (mesma regra do minimapa).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class WaypointBeacons {
    private static final org.joml.Matrix4f PROJ = new org.joml.Matrix4f(), VIEW = new org.joml.Matrix4f();
    private static Vec3 CAM = Vec3.ZERO;
    private static boolean haveMatrices = false;

    public static final net.minecraft.resources.ResourceLocation LABELS_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "waypoint_labels");

    // ponto do mundo -> tela (interface); null se atras da camera
    static float[] toScreen(double x, double y, double z, int gw, int gh) {
        if (!haveMatrices) return null;
        org.joml.Vector4f v = new org.joml.Vector4f((float) (x - CAM.x), (float) (y - CAM.y), (float) (z - CAM.z), 1f);
        VIEW.transform(v);
        PROJ.transform(v);
        if (v.w <= 0.05f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * gw, (0.5f - v.y / v.w * 0.5f) * gh};
    }

    // rotulo de cada marcacao na tela ("House (40 m)" com o icone), na direcao dela no mundo
    public static void renderLabels(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!haveMatrices || mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        MapOptions.ensureLoaded();
        if (!MapOptions.worldBeams || !com.bino.binosvanillaplus.map.MapGate.hasMap(mc.player)) return;
        float ts = MinimapHud.smallScale(mc);
        int icon = Math.max(8, Math.round(16 * ts));
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (w.hidden) continue;
            double dx = w.x + 0.5 - CAM.x, dy = w.y + 1.2 - CAM.y, dz = w.z + 0.5 - CAM.z;
            double dist = Math.sqrt(mc.player.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5));
            if (dist < 4) continue;
            org.joml.Vector4f v = new org.joml.Vector4f((float) dx, (float) dy, (float) dz, 1f);
            VIEW.transform(v);
            PROJ.transform(v);
            if (v.w <= 0.05f) continue;   // atras da camera
            float sx = (v.x / v.w * 0.5f + 0.5f) * g.guiWidth(), sy = (0.5f - v.y / v.w * 0.5f) * g.guiHeight();
            if (sx < -50 || sx > g.guiWidth() + 50 || sy < -20 || sy > g.guiHeight() + 20) continue;
            String label = w.name + (MapOptions.showDistance ? " (" + (int) dist + " m)" : "");
            float tw = mc.font.width(label) * ts;
            int bw = (int) Math.ceil(tw) + icon + 6, bh = icon + 2;
            int x = (int) (sx - bw / 2f), y = (int) (sy - bh / 2f);
            g.fill(x, y, x + bw, y + bh, 0x80303030);
            WaypointIcons.badge(g, w.icon(), x + 1, y + 1, icon, w.color);
            com.bino.binosvanillaplus.client.ui.BinoUi.text(g, mc.font, label, x + icon + 4, y + 1 + (icon - 8 * ts) / 2f, ts, 0xFFFFFFFF, false);
        }
    }

    private static int lighten(int rgb, float k) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r += (255 - r) * k; g += (255 - g) * k; b += (255 - b) * k;
        return (r << 16) | (g << 8) | b;
    }

    // coluna quadrada de raio r em volta do eixo, de y0 a y1
    private static void pillar(org.joml.Matrix4f m, com.mojang.blaze3d.vertex.VertexConsumer vc, float r, int rgb, int alpha, float y0, float y1) {
        int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
        float cx = 0.5f, cz = 0.5f;
        float[][] c = {{cx - r, cz - r}, {cx + r, cz - r}, {cx + r, cz + r}, {cx - r, cz + r}};
        for (int i = 0; i < 4; i++) {
            float[] a = c[i], b = c[(i + 1) % 4];
            vc.addVertex(m, a[0], y0, a[1]).setColor(cr, cg, cb, alpha);
            vc.addVertex(m, b[0], y0, b[1]).setColor(cr, cg, cb, alpha);
            vc.addVertex(m, b[0], y1, b[1]).setColor(cr, cg, cb, alpha);
            vc.addVertex(m, a[0], y1, a[1]).setColor(cr, cg, cb, alpha);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        // guarda as matrizes da camera para os rotulos (desenhados depois, no HUD)
        PROJ.set(event.getProjectionMatrix());
        VIEW.set(event.getModelViewMatrix());
        CAM = event.getCamera().getPosition();
        haveMatrices = true;
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        MapOptions.ensureLoaded();
        if (!MapOptions.worldBeams || !com.bino.binosvanillaplus.map.MapGate.hasMap(mc.player)) return;
        if (MapWaypoints.list().isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long time = mc.level.getGameTime();
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (w.hidden) continue;
            double dx = w.x + 0.5 - cam.x, dz = w.z + 0.5 - cam.z;
            double dist2 = dx * dx + dz * dz;
            if (dist2 < 4.0 * 4.0) continue;   // colado na marcacao o feixe atrapalha a visao
            ps.pushPose();
            ps.translate(w.x - cam.x, w.y - cam.y, w.z - cam.z);
            // largura cresce com a distancia para o feixe continuar visivel de longe
            float grow = (float) Math.min(4.0, Math.max(1.0, Math.sqrt(dist2) / 60.0));
            // feixe SOLIDO e intenso (o do beacon tem textura vazada): coluna na cor da marcacao + nucleo mais claro
            float r = 0.13f * grow;
            int rgb = w.color & 0xFFFFFF;
            int core = lighten(rgb, 0.55f);
            net.minecraft.client.renderer.RenderType rt = net.minecraft.client.renderer.RenderType.debugQuads();
            pillar(ps.last().pose(), buf.getBuffer(rt), r, rgb, 235, 0f, 400f);
            pillar(ps.last().pose(), buf.getBuffer(rt), r * 0.45f, core, 255, 0f, 400f);
            ps.popPose();
        }
        buf.endBatch();
    }
}
