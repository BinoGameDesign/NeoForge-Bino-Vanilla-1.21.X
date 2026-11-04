package com.bino.binosvanillaplus.client.leveling;

import com.bino.binosvanillaplus.client.lookat.LookAtInfo;
import com.bino.binosvanillaplus.client.lookat.LookAtProvider;
import com.bino.binosvanillaplus.leveling.MobLevels;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import com.bino.binosvanillaplus.network.MobLevelPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

// Nivel dos mobs no cliente: guarda o que o servidor manda e mostra "Nv. 23" no painel de inspecao
public final class LevelClient {
    private LevelClient() {}

    public static void receive(MobLevelPayload p) {
        var level = Minecraft.getInstance().level;
        Entity e = level == null ? null : level.getEntity(p.entityId());
        if (e != null) e.setData(ModAttachments.MOB_LEVEL.get(), p.level());
    }

    // cor da faixa de nivel: verde (baixo) -> amarelo -> laranja -> vermelho -> roxo (muito alto)
    public static int bandColor(double level) {
        int[] c = {0x6FD36F, 0xB8E05A, 0xF2D649, 0xF2A23C, 0xE8613A, 0xC93838, 0x9B3BC4};
        double t = Math.max(0, Math.min(0.9999, (level - 1) / 100.0)) * (c.length - 1);
        int i = (int) t;
        double f = t - i;
        int a = c[i], b = c[i + 1];
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static final class Provider implements LookAtProvider {
        @Override
        public void appendEntity(LookAtInfo info, Level level, Entity entity, LocalPlayer player) {
            int lv = MobLevels.get(entity);
            if (lv <= 0) return;
            info.line(Component.literal("Nv. " + lv).withStyle(s -> s.withColor(bandColor(lv) & 0xFFFFFF)).withStyle(ChatFormatting.BOLD));
        }
    }
}
