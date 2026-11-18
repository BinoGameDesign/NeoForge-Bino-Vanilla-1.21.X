package com.bino.binosvanillaplus.client.trap;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.custom.CageBlock;
import com.bino.binosvanillaplus.block.entity.CageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

// Porta da gaiola (desce/sobe em 6 ticks, como uma guilhotina), a isca no prato e o bicho preso la dentro
// (o proprio mob, com a animacao parada dele).
public class CageRenderer implements BlockEntityRenderer<CageBlockEntity> {
    public static final ModelResourceLocation DOOR = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/cage_door"));
    private static final float DOOR_TICKS = 6f, DOOR_DROP = 6.5f / 16f;

    public CageRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CageBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof CageBlock) || be.getLevel() == null) return;
        boolean closed = state.getValue(CageBlock.CLOSED);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-state.getValue(CageBlock.FACING).toYRot() + 180f));
        ps.translate(-0.5, 0, -0.5);

        // porta: aberta la em cima; fechando desce acelerando
        float t = Mth.clamp((be.getLevel().getGameTime() + partial - be.doorStart) / DOOR_TICKS, 0f, 1f);
        float down = closed ? t * t : 1f - t;
        ps.pushPose();
        ps.translate(0, -DOOR_DROP * down, 0);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(RenderType.cutout()), state, mc.getModelManager().getModel(DOOR), 1f, 1f, 1f, light, overlay);
        ps.popPose();

        if (!be.getBait().isEmpty() && !closed) {      // isca no prato
            ps.pushPose();
            ps.translate(8 / 16.0, 1.6 / 16.0, 12.5 / 16.0);
            ps.mulPose(Axis.XP.rotationDegrees(90));
            ps.scale(0.3f, 0.3f, 0.3f);
            mc.getItemRenderer().renderStatic(be.getBait(), ItemDisplayContext.FIXED, light, overlay, ps, buf, be.getLevel(), 0);
            ps.popPose();
        }

        if (be.hasCreature()) {                         // bicho preso
            if (be.dummy == null) be.dummy = EntityType.create(be.getCreature(), be.getLevel()).orElse(null);
            Entity e = be.dummy;
            if (e != null) {
                e.tickCount = (int) be.getLevel().getGameTime();   // a animacao parada anda
                float yaw = 180f;                                   // olhando para a porta
                e.setYRot(yaw); e.yRotO = yaw;
                if (e instanceof LivingEntity le) { le.yBodyRot = le.yBodyRotO = yaw; le.yHeadRot = le.yHeadRotO = yaw; }
                float s = Math.min(1f, 0.42f / Math.max(0.1f, e.getBbHeight()));   // cabe na gaiola
                ps.pushPose();
                ps.translate(0.5, 1 / 16.0, 0.55);
                ps.scale(s, s, s);
                mc.getEntityRenderDispatcher().render(e, 0, 0, 0, yaw, partial, ps, buf, light);
                ps.popPose();
            }
        }
        ps.popPose();
    }
}
