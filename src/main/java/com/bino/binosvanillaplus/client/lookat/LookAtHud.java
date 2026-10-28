package com.bino.binosvanillaplus.client.lookat;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.ModList;

// Painel "olhando para": mostra nome, mod e detalhes do bloco/entidade sob a mira.
public class LookAtHud {
    public static boolean enabled = true;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.level == null || mc.options.hideGui
                || mc.screen != null || mc.getDebugOverlay().showDebugScreen()) return;

        LookAtInfo info = collect(mc);
        if (info == null) return;
        draw(g, mc.font, info);
    }

    private static LookAtInfo collect(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null) return null;
        LookAtInfo info = new LookAtInfo();

        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) return null;
            ItemStack icon = state.getBlock().getCloneItemStack(mc.level, pos, state);
            if (icon.isEmpty()) icon = new ItemStack(state.getBlock());
            info.icon = icon;
            info.title = state.getBlock().getName();
            info.modName = modName(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
            for (LookAtProvider p : LookAtRegistry.PROVIDERS) p.appendBlock(info, mc.level, pos, state, mc.player);
            return info;
        }
        if (hit instanceof EntityHitResult ehr && hit.getType() == HitResult.Type.ENTITY) {
            Entity e = ehr.getEntity();
            SpawnEggItem egg = SpawnEggItem.byId(e.getType());
            if (egg != null) info.icon = new ItemStack(egg);
            info.title = e.getDisplayName();
            info.modName = modName(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
            for (LookAtProvider p : LookAtRegistry.PROVIDERS) p.appendEntity(info, mc.level, e, mc.player);
            return info;
        }
        return null;
    }

    private static Component modName(String ns) {
        String name = ns.equals("minecraft") ? "Minecraft"
                : ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName()).orElse(ns);
        return Component.literal(name).withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC);
    }

    private static void draw(GuiGraphics g, Font font, LookAtInfo info) {
        final int pad = 5, iconSize = 18, lineH = font.lineHeight + 2;
        boolean hasIcon = !info.icon.isEmpty();
        boolean hasBar = info.health >= 0;

        int textW = Math.max(font.width(info.title), font.width(info.modName));
        for (Component c : info.lines) textW = Math.max(textW, font.width(c));
        int w = pad * 2 + textW + (hasIcon ? iconSize + 4 : 0);
        int h = pad * 2 + lineH * (2 + info.lines.size()) - 2 + (hasBar ? 6 : 0);
        h = Math.max(h, pad * 2 + iconSize);

        int x = (g.guiWidth() - w) / 2, y = 6;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);       // borda
        g.fill(x, y, x + w, y + h, 0xCC101018);                         // fundo

        int tx = x + pad + (hasIcon ? iconSize + 4 : 0), ty = y + pad;
        if (hasIcon) g.renderItem(info.icon, x + pad, y + pad);
        g.drawString(font, info.title, tx, ty, 0xFFFFFFFF);
        ty += lineH;
        g.drawString(font, info.modName, tx, ty, 0xFFFFFFFF);
        ty += lineH;
        for (Component c : info.lines) {
            g.drawString(font, c, tx, ty, 0xFFFFFFFF);
            ty += lineH;
        }
        if (hasBar) {
            int bw = w - pad * 2;
            float frac = info.maxHealth <= 0 ? 0 : Math.min(1f, info.health / info.maxHealth);
            g.fill(x + pad, y + h - pad - 2, x + pad + bw, y + h - pad + 2, 0xFF3A0A0A);
            g.fill(x + pad, y + h - pad - 2, x + pad + (int) (bw * frac), y + h - pad + 2, 0xFFE03030);
        }
    }
}
