package com.bino.binosvanillaplus.client.journal;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

// Texturas das fotos (do PNG guardado no item ou no diario), com cache
public final class PhotoTextures {
    private PhotoTextures() {}

    public static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/item/photo.png");
    private static final int MAX = 96;
    private static final Map<Integer, ResourceLocation> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static int counter = 0;

    public static ResourceLocation get(byte[] png) {
        if (png == null || png.length == 0) return FALLBACK;
        int key = Arrays.hashCode(png);
        ResourceLocation id = CACHE.get(key);
        if (id != null) return id;
        try {
            NativeImage img = NativeImage.read(png);
            id = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "photo/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
        } catch (Exception e) {
            id = FALLBACK;
        }
        CACHE.put(key, id);
        if (CACHE.size() > MAX) {   // libera a mais antiga
            var it = CACHE.entrySet().iterator();
            var old = it.next();
            it.remove();
            if (!old.getValue().equals(FALLBACK)) Minecraft.getInstance().getTextureManager().release(old.getValue());
        }
        return id;
    }
}
