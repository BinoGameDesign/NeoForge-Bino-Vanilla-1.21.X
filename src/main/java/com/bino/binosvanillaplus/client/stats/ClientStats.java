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
    }

    @SubscribeEvent
    public static void onHud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.hasExperience()) return;
        GuiGraphics g = e.getGuiGraphics();
        var font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        // nivel do personagem: grande (2x), no meio, acima da barra de experiencia e por cima do resto do visor
        String lv = String.valueOf(level);
        g.pose().pushPose();
        g.pose().translate(w / 2f - font.width(lv), h - 31 - 22, 200);
        g.pose().scale(2f, 2f, 1f);
        outlined(g, lv, 0, 0, 0xFFFFFF);
        g.pose().popPose();
        // barras completas: verde, a esquerda da barra de experiencia
        String bars = String.valueOf(mc.player.experienceLevel);
        outlined(g, bars, w / 2 - 91 - 3 - font.width(bars), h - 29 - 2, 0x80FF20);
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
