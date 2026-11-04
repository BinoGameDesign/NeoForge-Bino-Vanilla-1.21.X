package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.block.entity.GraveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

// Escreve o nome do jogador morto entre o Allay fantasma e a lapide (sempre virado para a camera)
public class GraveRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final Font font;

    public GraveRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(GraveBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        String name = be.getOwnerName();
        if (name == null || name.isEmpty()) return;
        ps.pushPose();
        ps.translate(0.5, 1.2, 0.5);
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.02f, -0.02f, 0.02f);
        font.drawInBatch(name, -font.width(name) / 2f, 0f, 0xFFFFFFFF, false, ps.last().pose(), buf,
                Font.DisplayMode.NORMAL, 0x55000000, 0xF000F0);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(GraveBlockEntity be) { return false; }
}
