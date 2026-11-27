package com.bino.binosvanillaplus.client.stats;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.leveling.StatsSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

// Nivel do personagem no cliente (vem do servidor) e o visor novo da experiencia (regras de 23/11):
//  - no lugar do numero verde do jogo, no meio, fica o NIVEL DO PERSONAGEM (branco, fonte do jogo);
//  - a esquerda da barra de experiencia, em verde, quantas BARRAS completas voce tem (a moeda para subir de nivel).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientStats {
    private ClientStats() {}

    private static int level = 1, str, vit, agi, intel, wil;

    public static void receive(StatsSyncPayload m) {
        level = Math.max(1, m.level()); str = m.str(); vit = m.vit(); agi = m.agi(); intel = m.intel(); wil = m.wil();
    }

    public static int level() { return level; }
    public static int strength() { return str; }
    public static int vitality() { return vit; }
    public static int agility() { return agi; }
    public static int intelligence() { return intel; }
    public static int will() { return wil; }
    public static int freePoints() { return Math.max(0, level - 1 - str - vit - agi - intel - wil); }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        lastBars = -1; animStart = -1; level = 1; str = vit = agi = intel = wil = 0; }

    // o numero do jogo sai (camada "experience_level")
    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (e.getName().getPath().equals("experience_level")) e.setCanceled(true);
        // barra de experiencia nova do Bino (mesmo canto esquerdo da do jogo)
        if (e.getName().getPath().equals("experience_bar")) {
            e.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gameMode != null && mc.gameMode.hasExperience() && mc.player.jumpableVehicle() == null) xpBar(e.getGuiGraphics(), mc);
        }
    }

    @SubscribeEvent
    public static void onHud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.hasExperience()) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        // nivel do personagem: no lugar do numero do jogo, com os digitos de nivel (cor pela dezena)
        // (27/11) maior e no meio do vao entre as faixas: armadura/coracoes a esquerda, sede/fome a direita
        HudDigits.level(g, level, w / 2f, h - 39.5f, 1.5f);
        // barras completas: digitos do Bino em verde, a esquerda da barra de experiencia
        if (mc.player.jumpableVehicle() == null) barsNumber(g, mc, w, h);
    }

    // ---------------------------------------------------------------- animacao de subir uma barra (desenho do Bino)
    // xp_bar_anim.png (tools/ui/barra_xp_animacao.png): 6 quadros de 190x9 a 9 quadros por segundo. Quadro 0 = numero
    // antigo, 1-4 = o numero "girando" (desenho do Bino, centrado no numero), 5 = numero novo; a barra brilha junto.
    private static final net.minecraft.resources.ResourceLocation XP_ANIM =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.bino.binosvanillaplus.BinosVanillaMod.MOD_ID, "textures/gui/xp_bar_anim.png");
    private static final int ANIM_FRAMES = 6, ANIM_FPS = 9, SHEET_W = 190, SHEET_H = 54, BAR_U = 11, NUM_CENTER_U = 6;
    private static int lastBars = -1, fromBars;
    private static long animStart = -1;

    /** quadro da animacao agora (0..5), ou -1 se nao esta tocando */
    private static int animFrame() {
        if (animStart < 0) return -1;
        int f = (int) ((net.minecraft.Util.getMillis() - animStart) * ANIM_FPS / 1000L);
        if (f >= ANIM_FRAMES) { animStart = -1; return -1; }
        return f;
    }

    private static void barsNumber(GuiGraphics g, Minecraft mc, int w, int h) {
        int bars = mc.player.experienceLevel;
        if (lastBars >= 0 && bars > lastBars) { fromBars = lastBars; animStart = net.minecraft.Util.getMillis(); }
        lastBars = bars;
        int f = animFrame();
        float right = w / 2f - 91 - 2, top = h - 29 - 1;
        if (f > 0 && f < ANIM_FRAMES - 1) {   // numero girando: o desenho do quadro, centrado onde fica o numero
            String now = String.valueOf(bars);
            float center = right - HudDigits.width(now) / 2f;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.blit(XP_ANIM, Math.round(center - NUM_CENTER_U), Math.round(top - 1), 0, f * 9, 8, 9, SHEET_W, SHEET_H);
            return;
        }
        String s = String.valueOf(f == 0 ? fromBars : bars);
        HudDigits.draw(g, s, right - HudDigits.width(s), top, 1f, 0x20FF27);
    }

    private static final net.minecraft.resources.ResourceLocation XP_BAR =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.bino.binosvanillaplus.BinosVanillaMod.MOD_ID, "textures/gui/xp_bar.png");
    public static final int XP_W = 179, XP_H = 5;

    // fundo inteiro e a parte cheia ate onde a experiencia chegou (desenho do Bino, 179x5)
    private static void xpBar(GuiGraphics g, Minecraft mc) {
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - 29;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(XP_BAR, x, y, 0, 0, XP_W, XP_H, XP_W, 10);
        int f = animFrame();
        if (f >= 0) {   // subiu uma barra: a barra cheia brilhando (quadro da animacao)
            g.blit(XP_ANIM, x, y, BAR_U, f * 9 + 2, XP_W, XP_H, SHEET_W, SHEET_H);
            return;
        }
        int fill = (int) (mc.player.experienceProgress * (XP_W + 1));
        if (fill > 0) g.blit(XP_BAR, x, y, 0, 5, Math.min(XP_W, fill), XP_H, XP_W, 10);
    }

    private static void outlined(GuiGraphics g, String s, int x, int y, int col) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, s, x + 1, y, 0, false);
        g.drawString(font, s, x - 1, y, 0, false);
        g.drawString(font, s, x, y + 1, 0, false);
        g.drawString(font, s, x, y - 1, 0, false);
        g.drawString(font, s, x, y, col, false);
    }
}
