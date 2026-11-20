package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

// Estamina leve (mod Combat): uma roda de pontinhos do lado esquerdo da mira, estilo Zelda, que some quando esta cheia.
//  - gastam: rolamento 25, golpe pesado 20. Golpe leve e parry nao gastam (o combo continua livre).
//  - volta rapido (cheia em ~1,5 s), depois de 0,45 s sem gastar. Sem estamina a roda pisca vermelha.
//  - parry perfeito devolve 25.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Stamina {
    private Stamina() {}

    public static final float MAX = 100, ROLL = 25, HEAVY = 20;
    private static float value = MAX, shown = MAX;
    private static long lastUse = -100000, emptyAt = -100000, fullAt = 0;

    private static long now() { return System.currentTimeMillis(); }

    public static boolean use(float cost) {
        if (value < cost) { emptyAt = now(); return false; }
        value -= cost;
        lastUse = now();
        return true;
    }

    public static void give(float v) { value = Math.min(MAX, value + v); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (Minecraft.getInstance().player == null) { value = MAX; return; }
        if (now() - lastUse > 450 && value < MAX) {
            value = Math.min(MAX, value + 3.4f);
            if (value >= MAX) fullAt = now();
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        long now = now();
        shown += (value - shown) * 0.35f;
        float alpha = value >= MAX ? 1f - Mth.clamp((now - fullAt - 700) / 300f, 0f, 1f) : 1f;
        boolean flash = now - emptyAt < 400;
        if (alpha <= 0.02f && !flash) return;
        if (flash) alpha = 1f;
        GuiGraphics g = e.getGuiGraphics();
        float cx = g.guiWidth() / 2f - 22, cy = g.guiHeight() / 2f;
        int dots = 24;
        float frac = shown / MAX;
        int a = (int) (alpha * 230);
        for (int i = 0; i < dots; i++) {
            double ang = -Math.PI / 2 - (i / (double) dots) * Math.PI * 2;   // comeca em cima, anda no sentido anti-horario
            int x = Math.round((float) (cx + Math.cos(ang) * 8)), y = Math.round((float) (cy + Math.sin(ang) * 8));
            boolean on = i < Math.ceil(frac * dots - 0.01);
            int col = flash && !on ? 0xE04040 : on ? (frac < 0.3f ? 0xF2C14E : 0x7CE04A) : 0x303830;
            int al = on ? a : (int) (a * 0.6f);
            g.fill(x - 1, y - 1, x + 1, y + 1, (al << 24) | col);
        }
    }
}
