package com.bino.binosvanillaplus.client.pocket;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.pocket.PocketDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// Visual da Dimensao Portatil: espaco vazio todo branco (sem ceu, sem sol, neblina branca)
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class PocketClientEvents {
    private PocketClientEvents() {}

    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pocket");

    static boolean inPocket() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(PocketDimension.LEVEL);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        // sem nuvens, sem ceu desenhado, luz sempre cheia (sala toda clara)
        event.register(EFFECTS, new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, true, false) {
            @Override
            public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) { return new Vec3(1.0, 1.0, 1.0); }

            @Override
            public boolean isFoggyAt(int x, int z) { return false; }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!inPocket() || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        event.setRed(1f);
        event.setGreen(1f);
        event.setBlue(1f);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fog(ViewportEvent.RenderFog event) {
        if (!inPocket() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) return;
        // o chao branco some na neblina branca: parece um vazio sem fim
        event.setNearPlaneDistance(10f);
        event.setFarPlaneDistance(46f);
        event.setCanceled(true);
    }
}
