package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Medidas da Mesa de Lapidacao, em pixels do modelo (0 a 16), como no Blockbench. O modelo foi feito virado para o
// OESTE (frente = lado -x, onde ficam o visor e o botao); o bloco gira conforme a direcao (FACING) igual ao blockstate.
public final class LapidaryGeo {
    private LapidaryGeo() {}

    // centro do disco (a parte cinza), no topo do disco
    public static final double DISC_X = 7.0, DISC_Y = 12.4, DISC_Z = 10.5;
    // minerio deitado em cima do disco
    public static final Vec3 ORE = new Vec3(7.0, 12.45, 10.5);
    // ponta do lapidador: face de baixo (o silex fica preso aqui); desce ARM_DROP pixels ao lapidar
    public static final Vec3 FLINT = new Vec3(7.25, 14.6, 11.0);
    public static final double ARM_DROP = 1.8;
    // botao (face da frente) e visor
    public static final Vec3 BUTTON = new Vec3(1.15, 8.0, 5.0);
    public static final double BUTTON_PRESS = 0.15;
    public static final Vec3 VISOR = new Vec3(1.28, 8.0, 2.65);
    // onde as gemas prontas sao deixadas (junto das gemas de enfeite)
    public static final Vec3 GEMS = new Vec3(10.2, 11.02, 5.6);
    // maos em descanso: nos cantos da mesa do lado do jogador (fora do caminho da cena)
    public static final Vec3 REST_RIGHT = new Vec3(0.2, 11.0, 16.4);
    public static final Vec3 REST_LEFT = new Vec3(0.2, 10.8, -0.4);
    // camera da cena (olho) e ponto para onde ela olha; onde o jogador fica de pe
    public static final Vec3 EYE = new Vec3(-5.6, 24.4, 8.0);
    public static final Vec3 LOOK = new Vec3(8.0, 12.0, 8.0);
    public static final Vec3 STAND = new Vec3(-7.5, 0, 8.0);
    // area do tampo (para a mao seguir o mouse)
    public static final double TOP_Y = 11.0, TOP_MIN_X = 2.2, TOP_MAX_X = 15.6, TOP_MIN_Z = 0.4, TOP_MAX_Z = 15.6;

    // modelos das pecas que se mexem (models/block/lapidary_table_<nome>.json)
    public static final ModelResourceLocation DISC = part("disc"), ARM = part("arm"), BUTTON_MODEL = part("button"), VISOR_MODEL = part("visor");

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/lapidary_table_" + name));
    }

    public static Direction facing(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.WEST;
    }

    // giro do blockstate: oeste 0, norte 90, leste 180, sul 270
    public static float yRot(Direction facing) {
        return switch (facing) { case NORTH -> 90f; case EAST -> 180f; case SOUTH -> 270f; default -> 0f; };
    }

    // pixel do modelo -> ponto no mundo
    public static Vec3 world(BlockPos pos, Direction facing, Vec3 px) {
        return world(pos, facing, px.x, px.y, px.z);
    }

    public static Vec3 world(BlockPos pos, Direction facing, double px, double py, double pz) {
        double x = px / 16.0 - 0.5, z = pz / 16.0 - 0.5, rx, rz;
        switch (facing) {
            case NORTH -> { rx = -z; rz = x; }
            case EAST -> { rx = -x; rz = -z; }
            case SOUTH -> { rx = z; rz = -x; }
            default -> { rx = x; rz = z; }
        }
        return new Vec3(pos.getX() + 0.5 + rx, pos.getY() + py / 16.0, pos.getZ() + 0.5 + rz);
    }

    // direcao do modelo -> direcao no mundo (so gira)
    public static Vec3 dir(Direction facing, double x, double y, double z) {
        return world(BlockPos.ZERO, facing, x * 16 + 8, y * 16, z * 16 + 8).subtract(0.5, 0, 0.5);
    }

    // poe o PoseStack no sistema do modelo (origem no canto do bloco, unidades de bloco)
    public static void applyFacing(PoseStack ps, Direction facing) {
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-yRot(facing)));
        ps.translate(-0.5, 0, -0.5);
    }
}
