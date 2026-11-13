package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Icone 2D de item +10 (inventario, hotbar...): silhueta de 1 pixel em volta do desenho, na cor principal do item
// (a mesma do brilho do ForgeGlow). Feita uma vez por item a partir do sprite textures/item/<nome>.png e guardada
// como textura; e' desenhada antes do item (GuiGraphicsForgeOutlineMixin), entao fica por tras dele.
public final class ForgeIconOutline {
    private ForgeIconOutline() {}

    private record Outline(ResourceLocation tex, int w, int h) {}
    private static final Outline NONE = new Outline(null, 0, 0);
    private static final Map<Item, Outline> CACHE = new HashMap<>();

    public static void draw(GuiGraphics g, ItemStack stack, int x, int y) {
        if (ForgeGlow.tier(stack) < 4) return;
        Outline o = CACHE.computeIfAbsent(stack.getItem(), ForgeIconOutline::build);
        if (o == NONE) return;
        RenderSystem.enableBlend();
        g.blit(o.tex, x - 1, y - 1, 18, 18, 0, 0, o.w, o.h, o.w, o.h);
    }

    public static void clearCache() { CACHE.clear(); }

    private static Outline build(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation src = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png");
        var res = Minecraft.getInstance().getResourceManager().getResource(src);
        if (res.isEmpty()) return NONE;   // item sem sprite 2D proprio (blocos etc.): sem silhueta
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth(), h = Math.min(img.getHeight(), w);   // textura animada: so o primeiro quadro
            int s = Math.max(1, w / 16);                                 // 1 pixel do icone (sprites HD: mais pixels)
            int W = w + 2 * s, H = h + 2 * s;
            int rgb = ForgeGlow.color(item);
            // ABGR: a cor do item, opaca
            int abgr = 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
            NativeImage out = new NativeImage(W, H, true);
            for (int yy = 0; yy < H; yy++) {
                for (int xx = 0; xx < W; xx++) {
                    int ox = xx - s, oy = yy - s;
                    boolean inside = ox >= 0 && oy >= 0 && ox < w && oy < h && (img.getPixelRGBA(ox, oy) >>> 24) > 16;
                    boolean near = false;
                    if (!inside) {
                        for (int dy = -s; dy <= s && !near; dy++)
                            for (int dx = -s; dx <= s && !near; dx++) {
                                int nx = ox + dx, ny = oy + dy;
                                near = nx >= 0 && ny >= 0 && nx < w && ny < h && (img.getPixelRGBA(nx, ny) >>> 24) > 16;
                            }
                    }
                    out.setPixelRGBA(xx, yy, near ? abgr : 0);
                }
            }
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
                    "forge_outline/" + id.getNamespace() + "/" + id.getPath());
            Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(out));
            return new Outline(tex, W, H);
        } catch (Exception e) {
            return NONE;
        }
    }
}
