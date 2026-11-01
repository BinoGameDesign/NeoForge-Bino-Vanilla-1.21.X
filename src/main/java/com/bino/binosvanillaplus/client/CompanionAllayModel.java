package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.allay.Allay;

// Modelo do Allay do jogo + 3 animacoes do pet (somadas por cima das animacoes normais dele):
//  - CARINHO: pulinhos, balanca o corpo e a cabeca, bracos para cima e asas batendo rapido
//  - APERTAR (botao/alavanca): inclina para frente, recolhe o braco e da um "toque" rapido
//  - PEGAR itens: mergulho com os bracos indo buscar e abracando; ENTREGAR: estende os bracos oferecendo e acena com a cabeca
public class CompanionAllayModel extends AllayModel {
    private final ModelPart root, head, body, rightArm, leftArm, rightWing, leftWing;

    public CompanionAllayModel(ModelPart part) {
        super(part);
        this.root = part.getChild("root");
        this.head = this.root.getChild("head");
        this.body = this.root.getChild("body");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
    }

    @Override
    public void setupAnim(Allay allay, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(allay, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        if (!(allay instanceof CompanionAllayEntity pet)) return;
        float pt = age - allay.tickCount;
        float PI = Mth.PI;

        // ---------------- carinho (1,5 s)
        float t = pet.animProgress(0, pt, 30f);
        if (t >= 0f) {
            float env = Mth.sin(PI * t);
            root.y -= Math.abs(Mth.sin(t * PI * 3f)) * 2.2f * env;           // 3 pulinhos
            root.zRot += Mth.sin(t * PI * 4f) * 0.22f * env;                 // balanca de felicidade
            head.zRot += Mth.sin(t * PI * 4f + 1.2f) * 0.35f * env;
            head.xRot -= 0.25f * env;                                        // olha para cima
            rightArm.xRot -= 2.5f * env; leftArm.xRot -= 2.5f * env;          // bracos para cima
            rightArm.zRot += 0.45f * env; leftArm.zRot -= 0.45f * env;
            float flap = Mth.sin(t * PI * 12f) * 0.7f * env;                 // asas batendo rapido
            rightWing.yRot += flap; leftWing.yRot -= flap;
        }

        // ---------------- apertar (0,7 s): recolhe, toca, volta
        t = pet.animProgress(1, pt, 14f);
        if (t >= 0f) {
            float env = Mth.sin(PI * t);
            float arm;
            if (t < 0.3f) arm = 0.7f * (t / 0.3f);                           // braco vai para tras
            else if (t < 0.5f) arm = Mth.lerp((t - 0.3f) / 0.2f, 0.7f, -1.9f); // toque para frente
            else arm = Mth.lerp((t - 0.5f) / 0.5f, -1.9f, 0f);               // volta
            rightArm.xRot += arm;
            leftArm.xRot += 0.3f * env;
            root.xRot += 0.35f * env;                                        // inclina para o bloco
            head.xRot += 0.2f * env;
        }

        // ---------------- pegar itens (0,7 s): mergulho e abraco
        t = pet.animProgress(2, pt, 14f);
        if (t >= 0f) {
            float env = Mth.sin(PI * t);
            root.xRot += 0.6f * env;
            root.y += 1.2f * env;                                            // desce um pouco
            float reach = t < 0.55f ? Mth.sin(PI * t / 0.55f * 0.5f) : 1f - (t - 0.55f) / 0.45f;
            rightArm.xRot -= 1.3f * reach; leftArm.xRot -= 1.3f * reach;
            float hug = Mth.clamp((t - 0.45f) / 0.3f, 0f, 1f) * env;         // fecha os bracos no fim
            rightArm.zRot -= 0.6f * hug; leftArm.zRot += 0.6f * hug;
        }

        // ---------------- entregar (0,9 s): oferece com os bracos e acena com a cabeca
        t = pet.animProgress(3, pt, 18f);
        if (t >= 0f) {
            float env = Mth.sin(PI * t);
            rightArm.xRot -= 1.5f * env; leftArm.xRot -= 1.5f * env;
            rightArm.zRot -= 0.25f * env; leftArm.zRot += 0.25f * env;
            head.xRot += Mth.sin(t * PI * 2f) * 0.45f * env;                 // aceno "toma!"
            root.yRot += Mth.sin(t * PI * 2f) * 0.25f * env;
            float flap = Mth.sin(t * PI * 6f) * 0.4f * env;
            rightWing.yRot += flap; leftWing.yRot -= flap;
        }
    }
}
