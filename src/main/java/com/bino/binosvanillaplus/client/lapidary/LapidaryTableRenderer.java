package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.block.entity.LapidaryTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Pecas da Mesa de Lapidacao que se mexem (disco girando, lapidador descendo, botao, visor aceso) e os itens em cima
// dela (silex na ponta do lapidador, minerio/gema no disco, gemas prontas). O resto da mesa e' o modelo normal do bloco.
public class LapidaryTableRenderer implements BlockEntityRenderer<LapidaryTableBlockEntity> {
    public LapidaryTableRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LapidaryTableBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = LapidaryGeo.facing(state);
        LapidaryView v = LapidaryScene.viewFor(be.getBlockPos());
        if (v == null) v = LapidaryRemote.view(be.getBlockPos());

        ps.pushPose();
        LapidaryGeo.applyFacing(ps, facing);

        // disco (gira em volta do centro) com o minerio/gema em cima
        ps.pushPose();
        ps.translate(LapidaryGeo.DISC_X / 16, 0, LapidaryGeo.DISC_Z / 16);
        ps.mulPose(Axis.YP.rotationDegrees(-v.angle()));
        ps.translate(-LapidaryGeo.DISC_X / 16, 0, -LapidaryGeo.DISC_Z / 16);
        part(ps, buf, state, LapidaryGeo.DISC, light, overlay);
        if (!v.disc().isEmpty()) {
            ps.pushPose();
            ps.translate(LapidaryGeo.ORE.x / 16, LapidaryGeo.ORE.y / 16 + 0.004, LapidaryGeo.ORE.z / 16);
            LapidaryDraw.flat(ps, buf, v.disc(), 0.26f * v.discScale(), 20f, light);
            ps.popPose();
        }
        ps.popPose();

        // lapidador (desce ao lapidar) com o silex preso na ponta, por baixo
        ps.pushPose();
        ps.translate(0, -v.armDrop() / 16, 0);
        part(ps, buf, state, LapidaryGeo.ARM, light, overlay);
        if (!v.flint().isEmpty()) {
            ps.pushPose();
            ps.translate(LapidaryGeo.FLINT.x / 16, LapidaryGeo.FLINT.y / 16, LapidaryGeo.FLINT.z / 16);
            LapidaryDraw.flat(ps, buf, v.flint(), 0.17f, 35f, light);
            ps.popPose();
        }
        ps.popPose();

        // botao (afunda) e visor (aceso quando da para lapidar)
        ps.pushPose();
        ps.translate(v.button() / 16, 0, 0);
        part(ps, buf, state, LapidaryGeo.BUTTON_MODEL, light, overlay);
        ps.popPose();
        part(ps, buf, state, LapidaryGeo.VISOR_MODEL, v.visor() ? LightTexture.FULL_BRIGHT : light, overlay);
        if (v.visor()) {
            float pulse = 0.22f + 0.12f * (float) Math.sin(LapidaryRemote.clock() * 4.0);
            ps.pushPose();
            ps.translate(LapidaryGeo.VISOR.x / 16, LapidaryGeo.VISOR.y / 16, LapidaryGeo.VISOR.z / 16);
            LapidaryDraw.glowRect(ps, buf, 2.4f / 16, 1.1f / 16, pulse);
            ps.popPose();
        }

        // silhueta branca: onde por o item da mao / o botao para apertar (so na propria cena)
        LapidaryScene.Target ghost = LapidaryScene.viewFor(be.getBlockPos()) != null ? LapidaryScene.ghost() : LapidaryScene.Target.NONE;
        if (ghost != LapidaryScene.Target.NONE) {
            boolean over = LapidaryScene.hover == ghost;
            float a = (over ? 0.55f : 0.32f) + 0.14f * (float) Math.sin(LapidaryRemote.clock() * 5.0);
            ItemStack held = LapidaryScene.carried();
            ps.pushPose();
            switch (ghost) {
                case ORE -> {
                    ps.translate(LapidaryGeo.ORE.x / 16, LapidaryGeo.ORE.y / 16 + 0.006, LapidaryGeo.ORE.z / 16);
                    LapidaryDraw.ghost(ps, buf, held, 0.26f, 20f, a);
                }
                case FLINT -> {
                    // o silex fica por baixo do lapidador: a silhueta inclina para a camera (que esta acima, no lado -x)
                    ps.translate(LapidaryGeo.FLINT.x / 16, (LapidaryGeo.FLINT.y - 0.4) / 16, LapidaryGeo.FLINT.z / 16);
                    ps.mulPose(Axis.ZP.rotationDegrees(50f));
                    LapidaryDraw.ghost(ps, buf, held, 0.17f, 35f, a);
                }
                default -> {
                    ps.translate((LapidaryGeo.BUTTON.x - 0.06) / 16, LapidaryGeo.BUTTON.y / 16, LapidaryGeo.BUTTON.z / 16);
                    LapidaryDraw.glowRect(ps, buf, 1.7f / 16, 1.7f / 16, a);
                }
            }
            ps.popPose();
        }

        // gemas lapidadas nesta visita, deixadas no canto das gemas
        List<ItemStack> pile = v.pile();
        for (int i = 0; i < Math.min(6, pile.size()); i++) {
            Vec3 at = LapidaryGeo.GEMS.add(PILE[i][0], 0.35 * i, PILE[i][1]);
            ps.pushPose();
            ps.translate(at.x / 16, at.y / 16 + 0.003, at.z / 16);
            LapidaryDraw.flat(ps, buf, pile.get(pile.size() - 1 - i), 0.19f, (float) PILE[i][2], light);
            ps.popPose();
        }
        ps.popPose();
    }

    // deslocamento (px) e giro de cada gema da pilha
    private static final double[][] PILE = {{0, 0, 15}, {0.8, -0.6, -40}, {-0.7, 0.5, 70}, {0.3, 0.8, -10}, {-0.4, -0.7, 120}, {0.6, 0.2, 50}};

    private static void part(PoseStack ps, MultiBufferSource buf, BlockState state, ModelResourceLocation id, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(id);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(RenderType.cutout()), state, model,
                1f, 1f, 1f, light, overlay);
    }

    @Override
    public boolean shouldRenderOffScreen(LapidaryTableBlockEntity be) { return false; }
}
