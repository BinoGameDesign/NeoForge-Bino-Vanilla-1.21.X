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
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { level = 1; str = vit = agi = intel = wil = 0; }

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
        HudDigits.level(g, level, w / 2f, h - 31 - 4 - 3);
        // barras completas: digitos do Bino em verde, a esquerda da barra de experiencia
        if (mc.player.jumpableVehicle() == null) {
            String bars = String.valueOf(mc.player.experienceLevel);
            HudDigits.draw(g, bars, w / 2f - 91 - 2 - HudDigits.width(bars), h - 29 - 1, 1f, 0x20FF27);
        }
    }

    private static final net.minecraft.resources.ResourceLocation XP_BAR =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.bino.binosvanillaplus.BinosVanillaMod.MOD_ID, "textures/gui/xp_bar.png");
    public static final int XP_W = 179, XP_H = 5;

    // fundo inteiro e a parte cheia ate onde a experiencia chegou (desenho do Bino, 179x5)
    private static void xpBar(GuiGraphics g, Minecraft mc) {
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - 29;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(XP_BAR, x, y, 0, 0, XP_W, XP_H, XP_W, 10);
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
