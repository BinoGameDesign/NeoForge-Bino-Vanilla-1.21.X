package com.bino.binosvanillaplus.client.chest;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

// Modelo de bau gerado do Blockbench (ossos: root, bottom, lid, lock, hinge, handle; no mimico tambem dentes e lingua).
// HierarchicalModel so para poder tocar as animacoes do Blockbench (KeyframeAnimations); a pose e' feita por quem desenha.
public class BinoChestModel extends HierarchicalModel<Entity> {
    private final ModelPart top;

    public BinoChestModel(ModelPart top) { this.top = top; }

    @Override public ModelPart root() { return top; }
    @Override public void setupAnim(Entity e, float a, float b, float c, float d, float f) {}

    public void reset() { root().getAllParts().forEach(ModelPart::resetPose); }

    // mostra/esconde partes pelo nome (dentes e lingua do mimico)
    public void scalePart(String name, float s) {
        var p = this.getAnyDescendantWithName(name);
        p.ifPresent(part -> { part.xScale = s; part.yScale = s; part.zScale = s; });
    }

    public void lidPitch(float radians) {
        getAnyDescendantWithName("lid").ifPresent(p -> p.xRot += radians);
    }
}
