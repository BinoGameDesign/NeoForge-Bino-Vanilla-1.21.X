package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Icone de bloco para as marcacoes do mapa, com SILHUETA na cor da marcacao em volta (como no personagem).
// Cada item e' desenhado uma vez fora da tela (o mesmo desenho do inventario), vira uma textura, e dela sai uma
// mascara de contorno. Se algo falhar, cai no desenho normal do item (sem silhueta).
public final class BlockIcons {
    private BlockIcons() {}

    private static final int UNITS = 24, PAD = 4, PX = 96;   // 24 unidades de GUI (o item ocupa 16, com 4 de margem) em 96 px
    private static final int OUTLINE_R = 5;                  // espessura da silhueta, em px da textura

    private record Icon(ResourceLocation icon, ResourceLocation outline) {}

    private static final Map<Item, Icon> CACHE = new HashMap<>();
    private static final Set<Item> FAILED = new HashSet<>();
    private static int counter = 0;

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Icon i : CACHE.values()) {
            try { mc.getTextureManager().release(i.icon()); mc.getTextureManager().release(i.outline()); } catch (Throwable ignored) {}
        }
        CACHE.clear();
        FAILED.clear();
    }

    // desenha o item em (x,y) com 'size' px (como o item de 16 px da GUI), com silhueta na cor 'argb'
    public static void draw(GuiGraphics g, Item item, float x, float y, float size, int argb) {
        Icon ic = get(g, item);
        float k = size / 16f;
        if (ic == null) {   // sem silhueta: o desenho normal do item
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(k, k, 1f);
            g.renderItem(new ItemStack(item), 0, 0);
            g.pose().popPose();
            return;
        }
        float[] prev = RenderSystem.getShaderColor().clone();
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().translate(x - PAD * k, y - PAD * k, 0);
        g.pose().scale(k, k, 1f);
        RenderSystem.setShaderColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, prev[3]);
        g.blit(ic.outline(), 0, 0, UNITS, UNITS, 0, 0, PX, PX, PX, PX);
        RenderSystem.setShaderColor(1f, 1f, 1f, prev[3]);
        g.blit(ic.icon(), 0, 0, UNITS, UNITS, 0, 0, PX, PX, PX, PX);
        g.pose().popPose();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
    }

    private static Icon get(GuiGraphics g, Item item) {
        Icon ic = CACHE.get(item);
        if (ic != null || FAILED.contains(item)) return ic;
        g.flush();   // termina o que a tela atual estava desenhando antes de trocar o alvo de desenho
        try {
            ic = build(item);
        } catch (Throwable t) {
            BinosVanillaMod.LOGGER.warn("Nao consegui montar o icone de {}", item, t);
            ic = null;
        }
        if (ic == null) FAILED.add(item); else CACHE.put(item, ic);
        return ic;
    }

    private static Icon build(Item item) {
        Minecraft mc = Minecraft.getInstance();
        TextureTarget rt = new TextureTarget(PX, PX, true, Minecraft.ON_OSX);
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        NativeImage img = new NativeImage(PX, PX, false);
        try {
            rt.setClearColor(0f, 0f, 0f, 0f);
            rt.clear(Minecraft.ON_OSX);
            rt.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f, UNITS, UNITS, 0f, 1000f, 21000f), VertexSorting.ORTHOGRAPHIC_Z);
            Matrix4fStack mvs = RenderSystem.getModelViewStack();
            mvs.pushMatrix();
            mvs.identity();
            mvs.translate(0f, 0f, -11000f);
            RenderSystem.applyModelViewMatrix();
            try {
                GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                gg.renderItem(new ItemStack(item), PAD, PAD);
                gg.flush();
            } finally {
                mvs.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
            RenderSystem.bindTexture(rt.getColorTextureId());
            img.downloadTexture(0, false);
            img.flipY();
        } finally {
            rt.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
        }

        // alfa pre-multiplicado -> normal; mascara do contorno (dilatacao circular do alfa)
        boolean[] solid = new boolean[PX * PX];
        boolean any = false;
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                int p = img.getPixelRGBA(x, y);   // ABGR
                int a = p >>> 24;
                if (a > 0 && a < 255) {
                    int r = Math.min(255, (p & 255) * 255 / a), gg = Math.min(255, ((p >> 8) & 255) * 255 / a), b = Math.min(255, ((p >> 16) & 255) * 255 / a);
                    img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gg << 8) | r);
                }
                if (a > 24) { solid[y * PX + x] = true; any = true; }
            }
        }
        if (!any) { img.close(); return null; }   // nada foi desenhado: usa o desenho normal

        NativeImage out = new NativeImage(PX, PX, true);
        int r2 = OUTLINE_R * OUTLINE_R;
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                boolean hit = false;
                for (int dy = -OUTLINE_R; dy <= OUTLINE_R && !hit; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= PX) continue;
                    for (int dx = -OUTLINE_R; dx <= OUTLINE_R; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= PX || dx * dx + dy * dy > r2) continue;
                        if (solid[yy * PX + xx]) { hit = true; break; }
                    }
                }
                out.setPixelRGBA(x, y, hit && !solid[y * PX + x] ? 0xFFFFFFFF : 0x00000000);   // so o anel: nada de tinta por cima do bloco
            }
        }

        int id = counter++;
        ResourceLocation iconId = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "blockicon/" + id + "_i");
        ResourceLocation outId = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "blockicon/" + id + "_o");
        DynamicTexture t1 = new DynamicTexture(img), t2 = new DynamicTexture(out);
        t1.setFilter(true, false);
        t2.setFilter(true, false);
        mc.getTextureManager().register(iconId, t1);
        mc.getTextureManager().register(outId, t2);
        return new Icon(iconId, outId);
    }
}
