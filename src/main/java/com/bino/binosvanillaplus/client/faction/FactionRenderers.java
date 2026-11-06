package com.bino.binosvanillaplus.client.faction;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vindicator;

// Mobs das faccoes: o MESMO renderizador e modelo do jogo (assim o Fresh Animations/EMF funciona neles),
// so com a textura recolorida a partir da original (RecolorTexture).
public final class FactionRenderers {
    private FactionRenderers() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, p); }

    public static final ResourceLocation GOLEM_TEX = id("textures/entity/faction/pillager_golem.png");
    public static final ResourceLocation GUARD_TEX = id("textures/entity/faction/villager_guard.png");

    private static void register(ResourceLocation target, ResourceLocation source, RecolorTexture.PixelMap fn) {
        Minecraft.getInstance().getTextureManager().register(target, new RecolorTexture(target, source, fn));
    }

    public static class PillagerGolemRenderer extends IronGolemRenderer {
        public PillagerGolemRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            register(GOLEM_TEX, ResourceLocation.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png"), RecolorTexture::pillagerGolem);
        }

        @Override
        public ResourceLocation getTextureLocation(IronGolem e) { return GOLEM_TEX; }
    }

    public static class VillagerGuardRenderer extends VindicatorRenderer {
        public VillagerGuardRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            register(GUARD_TEX, ResourceLocation.withDefaultNamespace("textures/entity/illager/vindicator.png"), RecolorTexture::villagerGuard);
        }

        @Override
        public ResourceLocation getTextureLocation(Vindicator e) { return GUARD_TEX; }
    }
}
