package com.bino.binosvanillaplus.client.regions;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.ui.Ease;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

// Titulo da regiao no canto inferior esquerdo ao chegar (no estilo de Breath of the Wild): linha fina que cresce e o nome
// aparecendo devagar, depois sumindo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class RegionTitle {
    private RegionTitle() {}

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "region_title");
    private static final long IN_MS = 900, HOLD_MS = 2800, OUT_MS = 1100;

    private static int shownId = -1, candidate = -1, stable = 0;
    private static String text;
    private static long start = -1;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) { shownId = candidate = -1; stable = 0; start = -1; return; }
        BiomeRegions.Region r = BiomeRegions.regionAtBlock(p.getBlockX(), p.getBlockZ());
        if (r == null) return;
        if (r.id != candidate) { candidate = r.id; stable = 0; return; }
        if (stable < 100) stable++;
        if (stable >= 15 && r.id != shownId) {   // so avisa depois de ficar um instante (nao pisca na divisa)
            shownId = r.id;
            text = r.name;
            start = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, RegionTitle::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (start < 0 || text == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        long t = System.currentTimeMillis() - start;
        if (t > IN_MS + HOLD_MS + OUT_MS) { start = -1; return; }
        float in = Ease.outCubic(t / (float) IN_MS);
        float out = t > IN_MS + HOLD_MS ? Ease.inCubic((t - IN_MS - HOLD_MS) / (float) OUT_MS) : 0f;
        float vis = in * (1f - out);
        if (vis < 0.02f) return;

        float scale = 1.2f;
        int tw = mc.font.width(text);
        float x = 20 - (1f - in) * 16f;               // desliza da esquerda
        float y = g.guiHeight() - 96;
        int a = Math.max(4, (int) (255 * vis));
        // linha fina em cima do nome, crescendo
        int lineW = (int) (tw * scale * Math.min(1f, in * 1.2f)) + 6;
        g.fill((int) x, (int) y - 5, (int) x + lineW, (int) y - 4, (a << 24) | 0xFFFFFF);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(mc.font, text, 0, 0, (a << 24) | 0xFFFFFF, true);
        g.pose().popPose();
    }
}
