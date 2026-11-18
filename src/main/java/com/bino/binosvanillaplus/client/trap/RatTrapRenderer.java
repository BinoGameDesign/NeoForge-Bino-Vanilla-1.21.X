package com.bino.binosvanillaplus.client.trap;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.custom.RatTrapBlock;
import com.bino.binosvanillaplus.block.entity.RatTrapBlockEntity;
import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// Desenha o arco de arame da ratoeira (gira 180 graus no eixo da mola ao fechar, em 4 ticks, com "pancada" no fim)
// e o queijo em cima da isca enquanto houver isca.
public class RatTrapRenderer implements BlockEntityRenderer<RatTrapBlockEntity> {
    public static final ModelResourceLocation BAIL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/rat_trap_bail"));
    private static final float SNAP_TICKS = 4f;
    private static ItemStack slice;

    public RatTrapRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RatTrapBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof RatTrapBlock)) return;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-state.getValue(RatTrapBlock.FACING).toYRot() + 180f));   // igual ao "y" do blockstate
        ps.translate(-0.5, 0, -0.5);

        float angle = 0f;
        if (state.getValue(RatTrapBlock.SPRUNG)) {
            float t = be.getLevel() == null ? 1f : (be.getLevel().getGameTime() + partial - be.snapStart) / SNAP_TICKS;
            t = Mth.clamp(t, 0f, 1f);
            angle = 180f * t * t;                       // acelera ate bater
            if (t >= 1f) angle = 180f;
        }
        ps.pushPose();
        ps.translate(0, 1.5 / 16.0, 8 / 16.0);          // eixo da mola
        ps.mulPose(Axis.XP.rotationDegrees(angle));
        ps.translate(0, -1.5 / 16.0, -8 / 16.0);
        BakedModel bail = mc.getModelManager().getModel(BAIL);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(RenderType.cutout()), state, bail, 1f, 1f, 1f, light, overlay);
        ps.popPose();

        if (be.getBait() > 0 && !state.getValue(RatTrapBlock.SPRUNG)) {   // queijinho na isca
            if (slice == null) slice = new ItemStack(ModItems.CHEESE_SLICE.get());
            ps.pushPose();
            ps.translate(8 / 16.0, 1.9 / 16.0, 12 / 16.0);
            ps.mulPose(Axis.XP.rotationDegrees(90));
            ps.scale(0.28f, 0.28f, 0.28f);
            mc.getItemRenderer().renderStatic(slice, ItemDisplayContext.FIXED, light, overlay, ps, buf, be.getLevel(), 0);
            ps.popPose();
        }
        ps.popPose();
    }
}
