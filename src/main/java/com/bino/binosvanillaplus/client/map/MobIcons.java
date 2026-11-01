package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

// Rostinho 2D dos mobs no minimapa e no mapa-mundi: recorta a FACE da frente da cabeca direto da textura do mob
// (a textura atual dele: variantes, pacotes de textura e skins aparecem certinho). Mob sem rosto mapeado = ponto
// colorido (vermelho hostil, azul amigavel), como antes.
public final class MobIcons {
    private MobIcons() {}

    // regiao da face na textura, em pixels do modelo (largura do modelo = modelW; a altura sai da proporcao do PNG)
    private record Face(int u, int v, int w, int h, int modelW, boolean hat, @Nullable ResourceLocation overlay) {}

    private static final Map<EntityType<?>, Face> FACES = new IdentityHashMap<>();
    private static final Map<ResourceLocation, Integer> TEX_H = new HashMap<>();   // altura em pixels do modelo

    private static void face(Face f, EntityType<?>... types) { for (EntityType<?> t : types) FACES.put(t, f); }

    static {
        Face humanoid = new Face(8, 8, 8, 8, 64, true, null);
        face(humanoid, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.GIANT,
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED);
        face(new Face(8, 8, 8, 8, 64, false, null), EntityType.CREEPER, EntityType.PIG, EntityType.SLIME, EntityType.BLAZE,
                EntityType.SNOW_GOLEM);
        face(new Face(8, 8, 8, 8, 64, false, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")),
                EntityType.ENDERMAN);
        face(new Face(40, 12, 8, 8, 64, false, null), EntityType.SPIDER, EntityType.CAVE_SPIDER);
        face(new Face(6, 6, 8, 8, 64, false, null), EntityType.COW, EntityType.MOOSHROOM);
        face(new Face(8, 8, 6, 6, 64, false, null), EntityType.SHEEP);
        face(new Face(3, 3, 4, 6, 64, false, null), EntityType.CHICKEN);
        face(new Face(4, 4, 6, 6, 64, false, null), EntityType.WOLF);
        face(new Face(5, 5, 5, 4, 64, false, null), EntityType.CAT, EntityType.OCELOT);
        face(new Face(7, 11, 8, 6, 48, false, null), EntityType.FOX);
        face(new Face(8, 8, 8, 10, 64, false, null), EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ZOMBIE_VILLAGER,
                EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.ILLUSIONER);
        face(new Face(8, 8, 10, 8, 64, false, null), EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN);
        face(new Face(8, 8, 8, 10, 128, false, null), EntityType.IRON_GOLEM);
        face(new Face(16, 16, 16, 16, 64, false, null), EntityType.GHAST);
        face(new Face(12, 12, 12, 16, 64, false, null), EntityType.SQUID, EntityType.GLOW_SQUID);
        face(new Face(16, 16, 12, 12, 64, false, null), EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
        face(new Face(5, 5, 5, 5, 32, false, null), EntityType.ALLAY, EntityType.VEX);
        face(new Face(7, 7, 7, 7, 128, false, null), EntityType.POLAR_BEAR);
    }

    // desenha o icone centrado em (cx, cy); size = lado do rosto em pixels de interface (sem a borda)
    public static void draw(GuiGraphics g, Entity e, int cx, int cy, int size) {
        Face f = FACES.get(e.getType());
        ResourceLocation tex = f == null ? null : texture(e);
        if (f == null || tex == null) { dot(g, e, cx, cy); return; }
        int texH = texHeight(tex, f.modelW);
        int w = size, h = Math.max(2, Math.round(size * f.h / (float) f.w));
        if (h > size) { h = size; w = Math.max(2, Math.round(size * f.w / (float) f.h)); }
        int x = cx - w / 2, y = cy - h / 2;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);   // contorno
        g.blit(tex, x, y, w, h, f.u, f.v, f.w, f.h, f.modelW, texH);
        if (f.hat) g.blit(tex, x, y, w, h, f.u + 32, f.v, f.w, f.h, f.modelW, texH);   // camada de cima (capacete/chapeu da skin)
        if (f.overlay != null) g.blit(f.overlay, x, y, w, h, f.u, f.v, f.w, f.h, f.modelW, texHeight(f.overlay, f.modelW));
    }

    private static void dot(GuiGraphics g, Entity e, int cx, int cy) {
        int col = e instanceof Enemy ? 0xFFFF3B30 : 0xFF8FD8FF;
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static ResourceLocation texture(Entity e) {
        try {
            EntityRenderer<Entity> r = (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
            return r == null ? null : r.getTextureLocation(e);
        } catch (Throwable t) {
            return null;
        }
    }

    // altura da textura em "pixels do modelo": le so o cabecalho do PNG (largura/altura) uma vez
    private static int texHeight(ResourceLocation tex, int modelW) {
        return TEX_H.computeIfAbsent(tex, k -> {
            try {
                Optional<net.minecraft.server.packs.resources.Resource> res = Minecraft.getInstance().getResourceManager().getResource(k);
                if (res.isPresent()) {
                    try (InputStream in = res.get().open()) {
                        byte[] b = in.readNBytes(24);
                        if (b.length == 24) {
                            int w = ((b[16] & 255) << 24) | ((b[17] & 255) << 16) | ((b[18] & 255) << 8) | (b[19] & 255);
                            int h = ((b[20] & 255) << 24) | ((b[21] & 255) << 16) | ((b[22] & 255) << 8) | (b[23] & 255);
                            if (w > 0 && h > 0) return Math.max(1, Math.round(modelW * h / (float) w));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return modelW;   // desconhecido: quadrada
        });
    }

    // texturas podem mudar com pacote de textura
    public static void clearCache() { TEX_H.clear(); }
}
