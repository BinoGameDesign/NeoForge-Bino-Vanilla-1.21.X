package com.bino.binosvanillaplus.client.chest;

import com.bino.binosvanillaplus.mimic.MimicChest;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

// Desenha os baus do jogo (normal, armadilha, natal, ender; simples e duplo) com os modelos do Bino.
//  - O bau duplo e' desenhado inteiro pela metade LEFT (a metade RIGHT nao desenha nada).
//  - Tampa: a animacao "open" do Blockbench tocada conforme a abertura do jogo (0..1); fechando, toca a "close".
//  - Mimico disfarcado: e' o mesmo bau; so ganha uma "respiracao" de leve quando o servidor manda (MimicChest).
public class BinoChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T> {
    private final Map<ChestModels.Kind, BinoChestModel> models = new EnumMap<>(ChestModels.Kind.class);
    private static final Vector3f CACHE = new Vector3f();
    private static final Map<BlockEntity, float[]> LAST = new WeakHashMap<>();   // {abertura anterior, fechando?}

    public BinoChestRenderer(BlockEntityRendererProvider.Context ctx) {
        for (ChestModels.Kind k : ChestModels.Kind.values()) models.put(k, new BinoChestModel(ctx.bakeLayer(k.layer)));
    }

    @Override
    public void render(T be, float partialTick, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        ChestType type = st.hasProperty(ChestBlock.TYPE) ? st.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        if (type == ChestType.RIGHT) return;   // desenhado pela outra metade
        Direction facing = st.hasProperty(HorizontalDirectionalBlock.FACING) ? st.getValue(HorizontalDirectionalBlock.FACING) : Direction.SOUTH;

        ChestModels.Kind kind = be instanceof EnderChestBlockEntity ? ChestModels.Kind.ENDER
                : MimicChest.isChristmas() ? ChestModels.Kind.CHRISTMAS
                : be instanceof TrappedChestBlockEntity ? ChestModels.Kind.TRAPPED : ChestModels.Kind.NORMAL;
        Direction toPartner = null;
        float open = be.getOpenNess(partialTick);
        BlockPos pos = be.getBlockPos();
        if (type == ChestType.LEFT) {
            kind = kind.doubled();
            toPartner = ChestBlock.getConnectedDirection(st);
            if (be.getLevel() != null && be.getLevel().getBlockEntity(pos.relative(toPartner)) instanceof LidBlockEntity other)
                open = Math.max(open, other.getOpenNess(partialTick));
        }

        BinoChestModel model = models.get(kind);
        model.reset();
        float[] last = LAST.computeIfAbsent(be, k -> new float[]{open, 0});
        if (open < last[0] - 1e-4f) last[1] = 1; else if (open > last[0] + 1e-4f) last[1] = 0;
        last[0] = open;
        if (open > 0.001f) {
            AnimationDefinition def = last[1] > 0 ? kind.close : kind.open;
            float t = last[1] > 0 ? (1f - open) : open;
            KeyframeAnimations.animate(model, def, (long) (t * def.lengthInSeconds() * 1000f), 1f, CACHE);
        }
        if (be.getLevel() != null) {
            float br = MimicChest.breath(pos);
            if (toPartner != null) br = Math.max(br, MimicChest.breath(pos.relative(toPartner)));
            if (br > 0) model.lidPitch(br);
        }

        ps.pushPose();
        float ox = toPartner == null ? 0 : toPartner.getStepX() * 0.5f, oz = toPartner == null ? 0 : toPartner.getStepZ() * 0.5f;
        ps.translate(0.5f + ox, 0f, 0.5f + oz);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ps.translate(0f, 1.5f, 0f);
        ps.scale(-1f, -1f, 1f);
        model.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutout(kind.texture)), light, overlay);
        ps.popPose();
    }

    // o duplo passa da metade que desenha: caixa maior para nao sumir na borda da tela
    @Override
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(1.0);
    }
}
