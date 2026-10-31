package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
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
            BeaconRenderer.renderBeaconBeam(ps, buf, BeaconRenderer.BEAM_LOCATION, pt, 1.0f, time, 0, 400,
                    0xFF000000 | (w.color & 0xFFFFFF), 0.2f * grow, 0.25f * grow);
            ps.popPose();
        }
        buf.endBatch();
    }
}
