package com.bino.binosvanillaplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class CyalumeHud {

    // Desenha o texto no centro da tela, um pouco abaixo da mira
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        CyalumeClientState.expireIfNeeded();

        String text;
        int color;
        switch (CyalumeClientState.phase) {
            case FLYING -> {
                // Cronômetro em segundos e milissegundos
                long ms = (System.nanoTime() - CyalumeClientState.startNanos) / 1_000_000L;
                text = String.format(Locale.ROOT, "%d.%03d s", ms / 1000, ms % 1000);
                color = 0xFFFFFFFF;
            }
            case LANDED -> {
                // Depois de pousar, mostra a altura da queda no lugar do tempo
                text = String.format(Locale.ROOT, "%.2f blocos", CyalumeClientState.height);
                color = 0xFF55FFD5;
            }
            default -> {
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(graphics.guiWidth() / 2.0, graphics.guiHeight() / 2.0 + 14.0, 0.0);
        pose.scale(1.5f, 1.5f, 1.0f);   // texto 2x maior
        graphics.drawCenteredString(mc.font, text, 0, 0, color);
        pose.popPose();
    }
}