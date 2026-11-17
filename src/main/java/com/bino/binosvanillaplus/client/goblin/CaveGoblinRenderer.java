package com.bino.binosvanillaplus.client.goblin;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.goblin.CaveGoblinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CaveGoblinRenderer extends MobRenderer<CaveGoblinEntity, CaveGoblinModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/cave_goblin.png");

    public CaveGoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CaveGoblinModel(ctx.bakeLayer(CaveGoblinModel.LAYER)), 0.35f);
    }

    @Override
    public ResourceLocation getTextureLocation(CaveGoblinEntity entity) { return TEXTURE; }
}
