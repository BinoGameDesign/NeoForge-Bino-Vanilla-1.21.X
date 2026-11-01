package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.DyeColor;

// Igual ao desenho original do Allay, mas usa a textura pintada quando o pet foi tingido.
// Em volta dele: um brilho (aura) na COR do pet. Usa o tipo de desenho "olhos" do jogo (aditivo e sempre aceso),
// que o Complementary trata como emissivo -> brilha de verdade com shader.
public class CompanionAllayRenderer extends AllayRenderer {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
            "textures/entity/companion_allay/glow.png");

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

    // cor da luz do pet (RGB): a do corante, ou o azul do Allay
    public static int lightColor(CompanionAllayEntity pet) {
        int i = pet.getColorIndex();
        if (i < 0 || i > 15) return 0x5FD2FF;
        int c = DyeColor.byId(i).getTextureDiffuseColor() & 0xFFFFFF;
        // clareia um pouco (luz escura demais nao aparece)
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        int mx = Math.max(r, Math.max(g, b));
        if (mx > 0 && mx < 200) { float k = 200f / mx; r = Math.min(255, (int) (r * k)); g = Math.min(255, (int) (g * k)); b = Math.min(255, (int) (b * k)); }
        if (mx == 0) { r = g = b = 90; }
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(Allay allay, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(allay, yaw, pt, ps, buf, light);
        if (!(allay instanceof CompanionAllayEntity pet) || allay.isInvisible()) return;
        int c = lightColor(pet);
        float pulse = 0.85f + 0.15f * (float) Math.sin((allay.tickCount + pt) * 0.12f);
        int r = (int) (((c >> 16) & 255) * 0.55f * pulse), g = (int) (((c >> 8) & 255) * 0.55f * pulse), b = (int) ((c & 255) * 0.55f * pulse);
        ps.pushPose();
        ps.translate(0, allay.getBbHeight() * 0.55f, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float s = 0.9f;
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
