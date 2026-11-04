package com.bino.binosvanillaplus.client.journal;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix4f;

// Desenha a imagem da foto no item: no inventario, na mao e na moldura (ocupando a moldura toda, como um mapa)
public class PhotoItemRenderer extends BlockEntityWithoutLevelRenderer {
    public PhotoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ModDataComponents.PhotoData d = stack.get(ModDataComponents.PHOTO.get());
        ResourceLocation tex = d == null ? PhotoTextures.FALLBACK : PhotoTextures.get(d.png());
        Matrix4f m = ps.last().pose();
        // moldura de papel por baixo (borda branca) e a imagem por cima, dos dois lados (nunca aparece espelhada)
        VertexConsumer paper = buf.getBuffer(RenderType.text(PhotoTextures.FALLBACK));
        float b = 0.06f;
        face(paper, m, 0f, 0f, 1f, 1f, 0.5f - 0.002f, light, 0.14f, 0.14f, 0.17f, 0.17f);   // canto branco da textura de reserva
        face(paper, m, 0f, 0f, 1f, 1f, 0.5f + 0.002f, light, 0.14f, 0.14f, 0.17f, 0.17f);
        VertexConsumer vc = buf.getBuffer(RenderType.text(tex));
        face(vc, m, b, b, 1 - b, 1 - b, 0.5f + 0.004f, light, 0f, 0f, 1f, 1f);
        face(vc, m, b, b, 1 - b, 1 - b, 0.5f - 0.004f, light, 0f, 0f, 1f, 1f);
    }

    // retangulo nas duas faces: visto de +Z ou de -Z a imagem fica sempre na posicao certa
    private static void face(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int light,
                             float u0, float v0, float u1, float v1) {
        // frente (+Z)
        v(vc, m, x0, y0, z, u0, v1, light); v(vc, m, x1, y0, z, u1, v1, light);
        v(vc, m, x1, y1, z, u1, v0, light); v(vc, m, x0, y1, z, u0, v0, light);
        // costas (-Z), com U invertido
        v(vc, m, x0, y0, z, u1, v1, light); v(vc, m, x0, y1, z, u1, v0, light);
        v(vc, m, x1, y1, z, u0, v0, light); v(vc, m, x1, y0, z, u0, v1, light);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float vv, int light) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, vv).setLight(light);
    }

    @EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
    public static final class Registration {
        private static PhotoItemRenderer renderer;

        @SubscribeEvent
        public static void register(RegisterClientExtensionsEvent event) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) renderer = new PhotoItemRenderer();
                    return renderer;
                }
            }, ModItems.PHOTO.get());
        }
    }
}
