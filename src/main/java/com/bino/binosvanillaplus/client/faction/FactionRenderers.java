package com.bino.binosvanillaplus.client.faction;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.faction.VillagerMage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.WanderingTrader;

// Mobs das faccoes: o MESMO renderizador e modelo do jogo (assim o Fresh Animations/EMF funciona neles),
// so com a textura recolorida a partir da original (RecolorTexture).
public final class FactionRenderers {
    private FactionRenderers() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, p); }
    private static ResourceLocation mc(String p) { return ResourceLocation.withDefaultNamespace("textures/entity/" + p + ".png"); }

    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    // textura "source" recolorida com "fn"; registrada na primeira vez que alguem pede
    public static ResourceLocation recolor(String tag, ResourceLocation source, RecolorTexture.PixelMap fn) {
        return CACHE.computeIfAbsent(tag + "|" + source, k -> {
            ResourceLocation t = id("textures/entity/faction/" + tag + "/" + source.getNamespace() + "/" + source.getPath());
            Minecraft.getInstance().getTextureManager().register(t, new RecolorTexture(t, source, fn));
            return t;
        });
    }

    public static class PillagerGolemRenderer extends IronGolemRenderer {
        public PillagerGolemRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(IronGolem e) { return recolor("pillager_golem", mc("iron_golem/iron_golem"), RecolorTexture::pillagerGolem); }
    }

    public static class VillagerGuardRenderer extends VindicatorRenderer {
        public VillagerGuardRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(Vindicator e) { return recolor("villager_guard", mc("illager/vindicator"), RecolorTexture::villagerGuard); }
    }

    public static class VillagerMageRenderer extends EvokerRenderer<VillagerMage> {
        public VillagerMageRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(VillagerMage e) { return recolor("villager_mage", mc("illager/evoker"), RecolorTexture::villagerMage); }
    }

    public static class VillagerVexRenderer extends VexRenderer {
        public VillagerVexRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(Vex e) {
            return e.isCharging() ? recolor("villager_vex", mc("illager/vex_charging"), RecolorTexture::villagerSpirit)
                    : recolor("villager_vex", mc("illager/vex"), RecolorTexture::villagerSpirit);
        }
    }

    public static class VillagerWitchRenderer extends WitchRenderer {
        public VillagerWitchRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(Witch e) { return recolor("villager_witch", mc("witch"), RecolorTexture::villagerWitch); }
    }

    public static class VillagerRavagerRenderer extends RavagerRenderer {
        public VillagerRavagerRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(Ravager e) { return recolor("villager_ravager", mc("illager/ravager"), RecolorTexture::villagerRavager); }
    }

    public static class PillagerSmugglerRenderer extends WanderingTraderRenderer {
        public PillagerSmugglerRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public ResourceLocation getTextureLocation(WanderingTrader e) { return recolor("pillager_smuggler", mc("wandering_trader"), RecolorTexture::smuggler); }
    }

    // Pillager civil (villager do jogo): corpo e roupas (tipo, profissao, nivel) recoloridos
    public static ResourceLocation civilianBase() { return recolor("pillager_civilian", mc("villager/villager"), RecolorTexture::pillagerSkin); }

    public static ResourceLocation civilianClothes(ResourceLocation src) { return recolor("pillager_civilian", src, RecolorTexture::pillagerize); }
}
