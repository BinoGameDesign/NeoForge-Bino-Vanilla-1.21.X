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
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
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
