package com.bino.binosvanillaplus.client.faction;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vindicator;

// Desenho provisorio dos mobs das faccoes: o modelo e a textura do jogo com uma cor por cima.
// Quando chegar a arte final (textures/entity/...), e' so trocar a textura aqui.
public final class FactionRenderers {
    private FactionRenderers() {}

    private static int mul(int a, int b) {
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(a) * FastColor.ARGB32.alpha(b) / 255,
                FastColor.ARGB32.red(a) * FastColor.ARGB32.red(b) / 255,
                FastColor.ARGB32.green(a) * FastColor.ARGB32.green(b) / 255,
                FastColor.ARGB32.blue(a) * FastColor.ARGB32.blue(b) / 255);
    }

    // golem de ferro "enegrecido"
    public static class PillagerGolemRenderer extends IronGolemRenderer {
        public PillagerGolemRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            ModelPart root = ctx.bakeLayer(ModelLayers.IRON_GOLEM);
            this.model = new IronGolemModel<>(root) {
                @Override
                public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
                    super.renderToBuffer(ps, vc, light, overlay, mul(color, 0xFF55515C));
                }
            };
        }
    }

    // vingador com as cores da vila (tons de couro/terra no lugar do cinza dos illagers)
    public static class VillagerGuardRenderer extends VindicatorRenderer {
        public VillagerGuardRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            ModelPart root = ctx.bakeLayer(ModelLayers.VINDICATOR);
            this.model = new IllagerModel<Vindicator>(root) {
                @Override
                public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
                    super.renderToBuffer(ps, vc, light, overlay, mul(color, 0xFFD8C08E));
                }
            };
        }
    }
}
