package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.WaypointBeacons;
import com.bino.binosvanillaplus.network.WhistleSignalPayload;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

// Sinal do apito para quem ouviu: a silhueta branca de quem apitou aparece por alguns segundos, atraves de paredes.
// Se quem apitou esta longe demais para o jogo ter a entidade carregada, aparece uma silhueta branca (desenho) na
// direcao do ponto onde ele apitou. Sem distancia e sem acompanhar depois: e' so um sinal curto.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class WhistleSignals {
    private WhistleSignals() {}

    private static final long SHOW_MS = 4000;
    private record Mark(Vec3 pos, long at) {}
    private static final List<Mark> MARKS = new ArrayList<>();

    public static void receive(WhistleSignalPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entityId());
        if (e != null) ClientGlow.add(e.getId(), SHOW_MS);
        else MARKS.add(new Mark(new Vec3(p.x(), p.y(), p.z()), Util.getMillis()));
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "whistle_marks"), WhistleSignals::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (MARKS.isEmpty()) return;
        long now = Util.getMillis();
        MARKS.removeIf(m -> now - m.at() > SHOW_MS);
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        for (Mark m : MARKS) {
            float[] s = WaypointBeacons.toScreen(m.pos().x, m.pos().y + 1.0, m.pos().z, g.guiWidth(), g.guiHeight());
            if (s == null) continue;
            float t = (now - m.at()) / (float) SHOW_MS;
            float a = t < 0.1f ? t / 0.1f : t > 0.7f ? (1f - t) / 0.3f : 1f;
            figure(g, (int) s[0], (int) s[1], ((int) (a * 230) << 24) | 0xFFFFFF);
        }
    }

    // bonequinho branco (cabeca, corpo, bracos, pernas)
    private static void figure(GuiGraphics g, int cx, int cy, int c) {
        g.fill(cx - 2, cy - 12, cx + 2, cy - 8, c);   // cabeca
        g.fill(cx - 2, cy - 7, cx + 2, cy + 1, c);    // corpo
        g.fill(cx - 4, cy - 7, cx - 2, cy, c);        // bracos
        g.fill(cx + 2, cy - 7, cx + 4, cy, c);
        g.fill(cx - 2, cy + 1, cx, cy + 8, c);        // pernas
        g.fill(cx, cy + 1, cx + 2, cy + 8, c);
    }
}
