package com.bino.binosvanillaplus.client.combat;

import net.minecraft.util.Mth;
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
        float dk = CombatFx.deathLimbs(e);
        if (dk > 0.001f) { deathLimbs(model, e, dk); return; }
        float[] atk = CombatFx.attackPhase(e);
        if (atk != null) attackLimbs(model, atk[0], atk[1]);
        if (k <= 0.001f) return;
        float side = HitFx.side(e), front = HitFx.front(e);
        if (model instanceof HitReactive hr && hr.hitReact(k, side, front)) return;

        Map<ModelPart, Kind> parts = parts(model);

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

    private static Map<ModelPart, Kind> parts(EntityModel<?> model) {
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
        return parts;
    }

    // golpe do MOB (com o aviso do parry): no preparo os bracos sobem por cima da cabeca e a cabeca vai para tras;
    // no golpe os bracos descem com tudo para a frente e a cabeca acompanha. Mob sem bracos (aranha, lobo...) usa
    // so a cabeca e o corpo inteiro (CombatFx.onLivingPre).
    private static void attackLimbs(EntityModel<?> model, float raise, float strike) {
        float w = Math.max(raise, strike);
        if (w <= 0.001f) return;
        float tx = (raise * -2.6f + strike * -0.5f) / (raise + strike);   // braco: alvo entre "la em cima" e "batendo na frente"
        for (Map.Entry<ModelPart, Kind> en : parts(model).entrySet()) {
            ModelPart p = en.getKey();
            switch (en.getValue()) {
                case ARM_L -> rot(p, (tx - p.xRot) * w, 0, -0.22f * raise);
                case ARM_R -> rot(p, (tx - p.xRot) * w, 0, 0.22f * raise);
                case HEAD -> rot(p, -0.3f * raise + 0.32f * strike, 0, 0);
                case TAIL -> rot(p, -0.45f * raise + 0.2f * strike, 0, 0);
                default -> {}
            }
        }
    }

    // morte: normal = bracos para cima e cabeca para tras; stab = bracos agarrando a barriga e cabeca baixa;
    // queimado = bracos se debatendo; afogado/outras = tudo mole para baixo
    private static void deathLimbs(EntityModel<?> model, LivingEntity e, float dk) {
        Map<ModelPart, Kind> parts = new IdentityHashMap<>();
        ModelPart legL = null, legR = null;
        if (model instanceof HumanoidModel<?> h) {
            parts.put(h.head, Kind.HEAD); parts.put(h.leftArm, Kind.ARM_L); parts.put(h.rightArm, Kind.ARM_R);
            legL = h.leftLeg; legR = h.rightLeg;
        } else if (model instanceof HierarchicalModel<?> hm) collect(hm.root(), parts, 0);
        else if (model instanceof AgeableListModel<?> am) {
            for (ModelPart p : ((AgeableListModelAccessor) am).binosvanillaplus$headParts()) { parts.put(p, Kind.HEAD); break; }
            for (ModelPart p : ((AgeableListModelAccessor) am).binosvanillaplus$bodyParts()) collect(p, parts, 1);
        }
        if (model instanceof HeadedModel hd && !parts.containsKey(hd.getHead())) parts.put(hd.getHead(), Kind.HEAD);
        int kind = CombatFx.deathKind(e);
        float tm = e.tickCount * 0.9f;
        for (Map.Entry<ModelPart, Kind> en : parts.entrySet()) {
            ModelPart p = en.getKey();
            switch (kind) {
                case 1 -> {   // stab
                    switch (en.getValue()) {
                        case HEAD -> rot(p, 0.55f * dk, 0, 0);
                        case ARM_L -> rot(p, -1.05f * dk - p.xRot * dk, 0.35f * dk, 0.25f * dk);
                        case ARM_R -> rot(p, -1.05f * dk - p.xRot * dk, -0.35f * dk, -0.25f * dk);
                        case TAIL -> rot(p, 0.4f * dk, 0, 0);
                        default -> {}
                    }
                }
                case 2 -> {   // queimado: se debatendo
                    float f = Mth.sin(tm) * dk;
                    switch (en.getValue()) {
                        case HEAD -> rot(p, -0.3f * dk, 0.4f * f, 0);
                        case ARM_L -> rot(p, -1.4f * dk + 0.5f * f, 0, -0.4f * dk);
                        case ARM_R -> rot(p, -1.4f * dk - 0.5f * f, 0, 0.4f * dk);
                        case TAIL -> rot(p, 0.6f * f, 0, 0);
                        default -> {}
                    }
                }
                case 4, 5 -> {   // mole
                    switch (en.getValue()) {
                        case HEAD -> rot(p, 0.35f * dk, 0, 0.2f * dk);
                        case ARM_L, ARM_R -> rot(p, -p.xRot * dk, 0, 0);
                        default -> {}
                    }
                }
                default -> {   // normal / explosao: jogado para tras
                    switch (en.getValue()) {
                        case HEAD -> rot(p, -0.6f * dk, 0.15f * Mth.sin(tm * 0.5f) * dk, 0);
                        case ARM_L -> rot(p, -1.6f * dk, 0, -0.55f * dk);
                        case ARM_R -> rot(p, -1.6f * dk, 0, 0.55f * dk);
                        case TAIL -> rot(p, -0.6f * dk, 0, 0);
                        case EAR -> rot(p, -0.3f * dk, 0, 0);
                        default -> {}
                    }
                }
            }
        }
        if (legL != null && legR != null) {   // pernas: joelhos cedem (uma a frente, outra atras)
            float k2 = kind == 1 ? 0.9f * dk : 0.5f * dk;
            rot(legL, -k2 - legL.xRot * dk, 0, -0.1f * dk);
            rot(legR, 0.35f * k2 - legR.xRot * dk, 0, 0.1f * dk);
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
