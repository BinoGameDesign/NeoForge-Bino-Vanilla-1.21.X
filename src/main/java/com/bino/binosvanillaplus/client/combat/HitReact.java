package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.mixin.client.AgeableListModelAccessor;
import com.bino.binosvanillaplus.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// Reacao ao golpe RECEBIDO, para qualquer mob (do jogo, do mod e os que vierem), sem animacao feita a mao:
//  - o corpo inteiro: tranco para tras e volta, inclina para tras e da uma leve amassada (HitFx.onRenderPre);
//  - as pecas, achadas pelo NOME no modelo (ou pelos campos dos modelos do jogo):
//      cabeca ("head")  -> joga para tras e um pouco para o lado do golpe;
//      bracos           -> sobem e abrem (susto);
//      cauda ("tail")   -> levanta;
//      orelhas ("ear")  -> balancam.
// As mudancas sao aplicadas logo antes do desenho e DESFEITAS logo depois (mixin no LivingEntityRenderer), entao
// nao estragam a animacao do modelo. Mob novo: se as pecas tiverem esses nomes, ja funciona; para algo especial,
// o modelo implementa HitReactive.
public final class HitReact {
    private HitReact() {}

    private record Change(ModelPart part, float dx, float dy, float dz) {}
    private static final List<Change> APPLIED = new ArrayList<>();

    private enum Kind { HEAD, ARM_L, ARM_R, TAIL, EAR, OTHER }

    private static Kind kind(String n) {
        String s = n.toLowerCase(java.util.Locale.ROOT);
        if (s.equals("head")) return Kind.HEAD;
        if (s.matches("(left_?arm|arm_?l|arm_?left)")) return Kind.ARM_L;
        if (s.matches("(right_?arm|arm_?r|arm_?right)")) return Kind.ARM_R;
        if (s.equals("tail")) return Kind.TAIL;
        if (s.matches("(left_?ear|right_?ear|ear_?[lr]|ear_?(left|right)|ear)")) return Kind.EAR;
        return Kind.OTHER;
    }

    private static void rot(ModelPart p, float dx, float dy, float dz) {
        p.xRot += dx; p.yRot += dy; p.zRot += dz;
        APPLIED.add(new Change(p, dx, dy, dz));
    }

    // chamado logo antes de desenhar o modelo
    public static void apply(EntityModel<?> model, LivingEntity e) {
        APPLIED.clear();
        float k = HitFx.react(e);
        if (k <= 0.001f) return;
        float side = HitFx.side(e), front = HitFx.front(e);
        if (model instanceof HitReactive hr && hr.hitReact(k, side, front)) return;

        Map<ModelPart, Kind> parts = new IdentityHashMap<>();
        if (model instanceof HumanoidModel<?> h) {
            parts.put(h.head, Kind.HEAD);
            parts.put(h.leftArm, Kind.ARM_L);
            parts.put(h.rightArm, Kind.ARM_R);
        } else if (model instanceof HierarchicalModel<?> hm) {
            collect(hm.root(), parts, 0);
        } else if (model instanceof AgeableListModel<?> am) {
            for (ModelPart p : ((AgeableListModelAccessor) am).binosvanillaplus$headParts()) { parts.put(p, Kind.HEAD); break; }
            for (ModelPart p : ((AgeableListModelAccessor) am).binosvanillaplus$bodyParts()) collect(p, parts, 1);
        }
        if (model instanceof HeadedModel hd && !parts.containsKey(hd.getHead())) parts.put(hd.getHead(), Kind.HEAD);

        float back = front >= 0 ? 1f : -0.6f;   // golpe por tras: a cabeca vai para a frente (menos)
        for (Map.Entry<ModelPart, Kind> en : parts.entrySet()) {
            ModelPart p = en.getKey();
            switch (en.getValue()) {
                case HEAD -> rot(p, -0.42f * k * back, 0.18f * k * side, 0.22f * k * side);
                case ARM_L -> rot(p, -0.55f * k, 0, -0.35f * k);
                case ARM_R -> rot(p, -0.55f * k, 0, 0.35f * k);
                case TAIL -> rot(p, -0.5f * k, 0.2f * k * side, 0);
                case EAR -> rot(p, -0.3f * k, 0, 0.25f * k * side);
                default -> {}
            }
        }
    }

    // procura pecas com nomes conhecidos (sem entrar nas pecas "_r" de cubo girado, que sao filhas)
    private static void collect(ModelPart root, Map<ModelPart, Kind> out, int depth) {
        if (depth > 6) return;
        for (Map.Entry<String, ModelPart> c : ((ModelPartAccessor) (Object) root).binosvanillaplus$children().entrySet()) {
            Kind k = kind(c.getKey());
            if (k != Kind.OTHER && !out.containsKey(c.getValue())) out.put(c.getValue(), k);
            if (k != Kind.HEAD) collect(c.getValue(), out, depth + 1);   // dentro da cabeca so orelhas ja contam pela cabeca
        }
    }

    // chamado logo depois de desenhar: devolve as pecas para a pose da animacao
    public static void undo() {
        for (int i = APPLIED.size() - 1; i >= 0; i--) {
            Change c = APPLIED.get(i);
            c.part().xRot -= c.dx(); c.part().yRot -= c.dy(); c.part().zRot -= c.dz();
        }
        APPLIED.clear();
    }
}
