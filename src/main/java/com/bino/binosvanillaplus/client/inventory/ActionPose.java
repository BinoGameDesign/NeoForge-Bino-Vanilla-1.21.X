package com.bino.binosvanillaplus.client.inventory;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

// Poses de acao do personagem no inventario novo (uma diferente em cada aba). Angulos em radianos.
// O braco direito segura o item da mao principal; a armadura e o item acompanham os ossos do modelo.
public final class ActionPose {
    private ActionPose() {}

    public enum Pose { GUARD, HERO, STRIKE }

    public static Pose current;   // != null so enquanto o menu desenha o jogador

    public static void apply(PlayerModel<?> m, Pose p, float age) {
        float breathe = Mth.sin(age * 0.07f) * 0.03f;   // respiracao leve (nao fica duro)
        reset(m);
        switch (p) {
            case GUARD -> {   // em guarda: espada baixa e para tras, pernas abertas
                set(m.rightArm, -0.55f + breathe, 0.35f, 0.55f);
                set(m.leftArm, 0.30f - breathe, 0f, -0.30f);
                set(m.rightLeg, 0.28f, 0f, 0.10f);
                set(m.leftLeg, -0.30f, 0f, -0.10f);
                set(m.head, 0.08f, -0.20f, 0f);
                set(m.body, 0f, -0.12f, 0f);
            }
            case HERO -> {    // heroica: item erguido, a outra mao na cintura, olhando para cima
                set(m.rightArm, -2.75f + breathe, 0.10f, 0.20f);
                set(m.leftArm, -0.15f, 0f, -0.55f);
                set(m.rightLeg, 0.05f, 0f, 0.14f);
                set(m.leftLeg, -0.05f, 0f, -0.14f);
                set(m.head, -0.22f, 0.10f, 0f);
            }
            case STRIKE -> {  // golpe: braco para a frente, passo largo
                set(m.rightArm, -1.55f + breathe, -0.35f, 0.10f);
                set(m.leftArm, 0.65f, 0f, -0.15f);
                set(m.rightLeg, -0.55f, 0f, 0.05f);
                set(m.leftLeg, 0.55f, 0f, -0.05f);
                set(m.head, 0.05f, 0.30f, 0f);
                set(m.body, 0f, 0.25f, 0f);
            }
        }
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
    }

    private static void reset(PlayerModel<?> m) {
        for (ModelPart part : new ModelPart[]{m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) set(part, 0, 0, 0);
    }

    private static void set(ModelPart p, float x, float y, float z) { p.xRot = x; p.yRot = y; p.zRot = z; }
}
