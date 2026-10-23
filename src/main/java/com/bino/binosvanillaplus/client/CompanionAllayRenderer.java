package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;

// Igual ao desenho original do Allay, mas usa a textura pintada quando o pet foi tingido
public class CompanionAllayRenderer extends AllayRenderer {

    public CompanionAllayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Allay allay) {
        if (allay instanceof CompanionAllayEntity pet && pet.getColorIndex() >= 0) {
            return AllayColorTextures.get(pet.getColorIndex());   // textura recolorida
        }
        return super.getTextureLocation(allay);                   // textura original (azul)
    }
}
