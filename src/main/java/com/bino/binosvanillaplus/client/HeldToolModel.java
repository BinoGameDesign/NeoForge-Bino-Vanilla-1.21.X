package com.bino.binosvanillaplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.joml.Vector3f;

// Ferramenta com DOIS visuais: o icone 2D (pixel art) e o modelo 3D.
//  - Inventario/hotbar (GUI) e moldura na PAREDE ........ 2D
//  - Mao (1a e 3a pessoa, inclui mobs), item solto no chao, cabeca,
//    moldura no CHAO ou no TETO ........................... 3D
public class HeldToolModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel solid;   // o modelo 3D

    public HeldToolModel(BakedModel flat, BakedModel solid) {
        super(flat);
        this.solid = solid;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        BakedModel chosen = use3D(context, poseStack) ? solid : originalModel;
        return chosen.applyTransform(context, poseStack, leftHand);   // aplica o "display" do modelo escolhido e devolve ele
    }

    private static boolean use3D(ItemDisplayContext context, PoseStack poseStack) {
        return switch (context) {
            case GUI, NONE -> false;
            case FIXED -> isFloorOrCeilingFrame(poseStack);
            default -> true;
        };
    }

    // A moldura "olha" para fora da superficie onde esta presa. Na parede essa direcao e' horizontal;
    // no chao/teto ela aponta para cima/baixo.
    private static boolean isFloorOrCeilingFrame(PoseStack poseStack) {
        Vector3f facing = new Vector3f(0.0F, 0.0F, 1.0F);
        poseStack.last().normal().transform(facing);
        facing.normalize();
        return Math.abs(facing.y()) > 0.7F;
    }

    // Usado pelo item solto no chao para calcular a altura; usamos a do modelo 3D.
    @Override
    public ItemTransforms getTransforms() {
        return solid.getTransforms();
    }
}
