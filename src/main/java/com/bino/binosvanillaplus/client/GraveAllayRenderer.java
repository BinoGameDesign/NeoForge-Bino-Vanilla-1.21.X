package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;

// Allay fantasma do tumulo: o modelo do jogo (EMF/Fresh Animations tambem animam) com a textura cinza e branca
// e um brilho palido em volta.
public class GraveAllayRenderer extends AllayRenderer {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
            "textures/entity/companion_allay/glow.png");

    public GraveAllayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Allay allay) {
        return AllayColorTextures.get(AllayColorTextures.GHOST);
    }

    @Override
    public void render(Allay allay, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(allay, yaw, pt, ps, buf, light);
        float pulse = 0.8f + 0.2f * (float) Math.sin((allay.tickCount + pt) * 0.08f);
        int r = (int) (210 * 0.45f * pulse), g = (int) (225 * 0.45f * pulse), b = (int) (235 * 0.45f * pulse);
        ps.pushPose();
        ps.translate(0, allay.getBbHeight() * 0.55f, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float s = 0.8f;
        PoseStack.Pose pose = ps.last();
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(GLOW));
        vert(vc, pose, -s, -s, 0, 1, r, g, b);
        vert(vc, pose, s, -s, 1, 1, r, g, b);
        vert(vc, pose, s, s, 1, 0, r, g, b);
        vert(vc, pose, -s, s, 0, 0, r, g, b);
        ps.popPose();
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int r, int g, int b) {
        vc.addVertex(pose, x, y, 0f).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, 0f, 1f, 0f);
    }
}
