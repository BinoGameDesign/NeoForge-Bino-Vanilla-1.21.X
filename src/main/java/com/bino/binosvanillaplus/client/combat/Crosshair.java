package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.ShoulderCam;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

// Mira propria (pixel art do Bino) no lugar da mira do jogo, com aviso de golpe vindo de FORA da visao:
//  - golpe pelas costas (reto)          -> pisca o traco rosa embaixo
//  - golpe por tras/lado esquerdo       -> pisca o risco rosa da esquerda
//  - golpe por tras/lado direito        -> pisca o risco rosa da direita
// O aviso vale em qualquer camera (primeira pessoa, ombro, terceira); a mira branca aparece onde o jogo
// mostraria a mira (primeira pessoa) e no ombro (ShoulderCam desenha no ponto mirado).
// Cor: a parte branca e' desenhada como "branco menos metade do fundo": fundo escuro = branca,
// fundo muito claro = cinza (nunca some no ceu, na neve ou na areia). O rosa e' desenhado normal.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Crosshair {
    private Crosshair() {}

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/crosshair/" + n + ".png");
    }
    private static final ResourceLocation CROSS = tex("crosshair"), BACK = tex("hit_back"), LEFT = tex("hit_left"), RIGHT = tex("hit_right");

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    private static final float GRAY = 0.5f;           // quanto o fundo claro escurece a mira (0,5 = branco vira cinza medio)
    private static final long FLASH_MS = 700;         // quanto tempo o aviso pisca
    private static final long BLINK_MS = 120;         // cada piscada (aceso/apagado)
    private static final float BACK_ANGLE = 150f;     // a partir daqui (graus da frente) conta como "costas reto"

    private static long backAt = -100000, leftAt = -100000, rightAt = -100000;

    // chega do servidor (CombatFx, HIT_FROM): yaw do mundo de onde veio o golpe
    public static void hit(float srcYaw) {
        Minecraft mc = Minecraft.getInstance();
        float camYaw = mc.gameRenderer.getMainCamera().getYRot();
        float rel = Mth.wrapDegrees(srcYaw - camYaw);   // 0 = na frente, + = a direita
        // metade do campo de visao horizontal: se o golpe veio de dentro, voce viu, nao precisa de aviso
        double vfov = Math.toRadians(mc.options.fov().get());
        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        float half = (float) Math.toDegrees(Math.atan(Math.tan(vfov / 2) * aspect));
        if (Math.abs(rel) <= half) return;
        long now = Util.getMillis();
        if (Math.abs(rel) >= BACK_ANGLE) backAt = now;
        else if (rel > 0) rightAt = now;
        else leftAt = now;
    }

    private static float blink(long at, long now) {
        long t = now - at;
        if (t < 0 || t >= FLASH_MS) return 0f;
        if ((t / BLINK_MS) % 2 == 1) return 0f;
        return 1f - 0.5f * t / (float) FLASH_MS;
    }

    // a mira do jogo sai (a nossa entra logo acima); com o F3 aberto fica a do jogo (os eixos de depuracao)
    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (e.getName().equals(VanillaGuiLayers.CROSSHAIR) && !Minecraft.getInstance().getDebugOverlay().showDebugScreen()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "crosshair"), Crosshair::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (ShoulderCam.active()) return;   // no ombro quem desenha e' a ShoulderCam (no ponto mirado)
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        boolean first = mc.options.getCameraType().isFirstPerson();
        boolean debug = mc.getDebugOverlay().showDebugScreen();
        boolean spectator = mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR;
        draw(g, cx, cy, first && !debug && !spectator);
    }

    // desenha a mira com o ponto do meio em (x, y); cross = false so os avisos
    public static void draw(GuiGraphics g, int x, int y, boolean cross) {
        int ox = x - 8, oy = y - 10;   // o pontinho do meio da arte fica em (8, 10)
        g.flush();
        if (cross) {
            RenderSystem.enableBlend();
            // cor final = branco - GRAY * fundo  (canal alfa da tela nao muda)
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_SUBTRACT, GL14.GL_FUNC_ADD);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.SRC_ALPHA,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            g.setColor(1f, 1f, 1f, GRAY);
            g.blit(CROSS, ox, oy, 0, 0, 17, 17, 17, 17);
            g.flush();
            g.setColor(1f, 1f, 1f, 1f);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        long now = Util.getMillis();
        float b = blink(backAt, now), l = blink(leftAt, now), r = blink(rightAt, now);
        if (b + l + r <= 0f) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (b > 0) { g.setColor(1f, 1f, 1f, b); g.blit(BACK, ox, oy, 0, 0, 17, 17, 17, 17); }
        if (l > 0) { g.setColor(1f, 1f, 1f, l); g.blit(LEFT, ox, oy, 0, 0, 17, 17, 17, 17); }
        if (r > 0) { g.setColor(1f, 1f, 1f, r); g.blit(RIGHT, ox, oy, 0, 0, 17, 17, 17, 17); }
        g.flush();
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
