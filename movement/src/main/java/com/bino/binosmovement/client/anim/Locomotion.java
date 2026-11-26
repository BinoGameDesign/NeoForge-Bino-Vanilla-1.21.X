package com.bino.binosmovement.client.anim;

import com.bino.binosmovement.BinosMovement;
import com.bino.binosmovement.client.MovementHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Animacoes do dia a dia feitas no Blockbench (assets/binosmovement/movement_anims/player.json, vindas de
// tools/blockbench/movimento.bbmodel): parado, andar, correr, agachado, andar_agachado, pulo, queda, aterrissar,
// aterrissar_pesado. Cada jogador tem pesos que mudam suave entre elas (sem estalo).
//  - andar/correr: o ciclo segue a distancia andada (o pe nao desliza; devagar = passo devagar)
//  - pulo: toca uma vez ao sair do chao; queda: depois de um tempo no ar descendo
//  - aterrissar / aterrissar_pesado: por cima de tudo, ao tocar o chao (pesado a partir de 4 blocos de queda)
// Animacao que ainda nao existe no arquivo: fica a vida procedural (PlayerLife). Nadando, voando de elytra, montado
// ou dormindo: fica o jogo (por enquanto).
// Bracos: so quando estao livres (mao vazia ou segurando um item parado); usando item, batendo, escudo, arco: o jogo.
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class Locomotion {
    private Locomotion() {}

    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(BinosMovement.MOD_ID, "movement_anims/player.json");
    private static final String[] LOOPS = {"parado", "andar", "correr", "agachado", "andar_agachado", "queda", "pulo"};
    private static final float CYCLE = Mth.TWO_PI / 0.6662f;   // distancia de uma passada completa no relogio do jogo
    private static final float CAMERA_SHARE = 0.5f;             // quanto do balanco do corpo vai para a camera

    private static final class State {
        final float[] w = new float[LOOPS.length];
        long last;
        boolean ground = true;
        float air, jumpT = 99f, peakY = Float.NaN, vy;
        double lastY = Double.NaN;
        String land; float landT = 99f;
        float lastAge = -1;
        float exertion;          // cansaco (0..1): sobe correndo/pulando, desce parado -> respiracao mais rapida e funda
        float breath;            // fase da respiracao (radianos)
        final float seed;        // cada jogador com os proprios movimentos involuntarios
        State(int id) { seed = (id * 0.6180339f) % 1f * 100f; }
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();

    static Map<String, BbAnims.Anim> anims() { return BbAnims.load(FILE); }

    private static boolean skip(Player p) {
        return p.isSleeping() || p.isPassenger() || p.isFallFlying() || p.isVisuallySwimming() || p.isSwimming()
                || p.getAbilities().flying || p.isSpectator() || p.isAutoSpinAttack() || p.isInWater();
    }

    // pose do dia a dia agora (null = sem animacao do Blockbench para o estado atual)
    public static BbAnims.Sample sample(Player p, float pt) {
        Map<String, BbAnims.Anim> an = anims();
        if (an.isEmpty() || skip(p)) return null;
        State s = STATES.computeIfAbsent(p.getId(), State::new);
        float age = p.tickCount + pt;
        float dt = s.lastAge < 0 ? 0f : Mth.clamp((age - s.lastAge) / 20f, 0f, 0.25f);
        s.lastAge = age;
        if (dt > 0f) step(p, s, dt);

        float cyc = p.walkAnimation.position(pt) / CYCLE;
        float clock = age / 20f;
        BbAnims.Sample out = null;
        float total = 0f;
        for (int i = 0; i < LOOPS.length; i++) {
            if (s.w[i] < 0.002f) continue;
            BbAnims.Anim a = an.get(LOOPS[i]);
            if (a == null) continue;
            BbAnims.Sample si;
            switch (LOOPS[i]) {
                case "andar", "correr", "andar_agachado" -> si = BbAnims.at(a, (cyc - (float) Math.floor(cyc)) * a.length());
                case "pulo" -> si = BbAnims.at(a, Math.min(s.jumpT, a.length()));
                default -> si = BbAnims.loop(a, clock);
            }
            total += s.w[i];
            out = out == null ? si : BbAnims.mix(out, si, s.w[i] / total);
        }
        if (out == null) return null;
        // aterrissagem por cima (some no fim)
        if (s.land != null) {
            BbAnims.Anim a = an.get(s.land);
            if (a != null && s.landT < a.length()) {
                float k = Mth.clamp(1f - (s.landT - (a.length() - 0.12f)) / 0.12f, 0f, 1f);
                out = BbAnims.mix(out, BbAnims.at(a, s.landT), k);
            }
        }
        return out;
    }

    // pesos de cada animacao (seguem o estado com suavidade) e os gatilhos (pulo, aterrissagem)
    private static void step(Player p, State s, float dt) {
        boolean ground = p.onGround();
        double y = p.getY();
        // velocidade vertical pela posicao (vale tambem para os outros jogadores, que o cliente so ve andar)
        float vy = Double.isNaN(s.lastY) ? 0f : (float) ((y - s.lastY) / dt) / 20f;   // blocos por tick
        s.vy += (vy - s.vy) * Math.min(1f, dt * 30f);
        s.lastY = y;
        if (ground && !s.ground) {   // tocou o chao: a altura da queda escolhe a aterrissagem
            Map<String, BbAnims.Anim> an = anims();
            float fall = Float.isNaN(s.peakY) ? 0f : (float) (s.peakY - y);
            s.land = fall >= 4f && an.containsKey("aterrissar_pesado") ? "aterrissar_pesado" : fall >= 1.2f ? "aterrissar" : null;
            s.landT = 0f;
        }
        if (!ground && s.ground && s.vy > 0.05f) s.jumpT = 0f;   // saiu do chao subindo: pulo
        if (ground) s.peakY = Float.NaN;
        else s.peakY = Float.isNaN(s.peakY) ? (float) y : Math.max(s.peakY, (float) y);
        s.ground = ground;
        s.air = ground ? 0f : s.air + dt;
        float vyNow = s.vy;
        s.jumpT += dt;
        s.landT += dt;
        // cansaco e respiracao
        boolean sprint = p.isSprinting() && ground;
        s.exertion = Mth.clamp(s.exertion + (sprint ? 0.22f * dt : -0.06f * dt), 0f, 1f);
        if (s.land != null && s.landT < dt * 1.5f) s.exertion = Math.min(1f, s.exertion + ("aterrissar_pesado".equals(s.land) ? 0.15f : 0.04f));
        float rate = 0.26f + 0.55f * s.exertion;   // respiracoes por segundo (parado ~16 por minuto; cansado ~48)
        s.breath = (s.breath + dt * rate * Mth.TWO_PI) % (Mth.TWO_PI * 1000f);

        float speed = Math.min(1f, p.walkAnimation.speed());
        float moving = Mth.clamp(speed / 0.35f, 0f, 1f);
        boolean crouch = p.isCrouching();
        float[] target = new float[LOOPS.length];
        if (ground) {
            if (crouch) { target[3] = 1f - moving; target[4] = moving; }
            else if (p.isSprinting()) { target[0] = 1f - moving; target[2] = moving; }
            else { target[0] = 1f - moving; target[1] = moving; }
        } else if (s.jumpT < 0.45f && vyNow > -0.2f) target[6] = 1f;      // subindo do pulo
        else if (s.air > 0.25f && vyNow < -0.25f) target[5] = 1f;         // caindo
        else target[6] = 1f;                                              // no ar sem cair ainda: segura o fim do pulo
        float k = 1f - (float) Math.exp(-dt * 12f);
        for (int i = 0; i < target.length; i++) s.w[i] += (target[i] - s.w[i]) * k;
    }

    // pose no modelo (chamado no fim do setupAnim); false = nada do Blockbench agora (fica a vida procedural)
    public static boolean pose(HumanoidModel<?> m, Player p, float ageInTicks) {
        if (MovementHooks.stance(p)) return false;   // guarda de combate: fica a vida procedural (PlayerLife)
        float pt = Mth.clamp(ageInTicks - p.tickCount, 0f, 1f);
        BbAnims.Sample a = sample(p, pt);
        if (a == null) return false;
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        boolean mir = !right;
        m.body.zRot = 0f;
        BbAnims.part(m.body, a, "body", 1f, mir, 0, 0, 0, false);
        BbAnims.part(m.head, a, "head", 1f, mir, 0, 0, 0, true);
        BbAnims.part(right ? m.rightLeg : m.leftLeg, a, "right_leg", 1f, mir, right ? -1.9f : 1.9f, 12, 0, false);
        BbAnims.part(right ? m.leftLeg : m.rightLeg, a, "left_leg", 1f, mir, right ? 1.9f : -1.9f, 12, 0, false);
        boolean swinging = m.attackTime > 0f || p.isUsingItem();
        boolean mainFree = arm(m, a, right ? m.rightArm : m.leftArm, "right_arm", right ? m.rightArmPose : m.leftArmPose, mir, right ? -5 : 5, swinging);
        boolean offFree = arm(m, a, right ? m.leftArm : m.rightArm, "left_arm", right ? m.leftArmPose : m.rightArmPose, mir, right ? 5 : -5, swinging);
        State s = STATES.get(p.getId());
        if (s != null) {
            crouchFix(m, s);
            alive(m, p, s, ageInTicks / 20f, right ? mainFree : offFree, right ? offFree : mainFree);
        }
        return true;
    }

    // o agachar das animacoes ja abaixa o corpo inteiro (root): o abaixamento do jogo sai, senao abaixava duas vezes
    private static void crouchFix(HumanoidModel<?> m, State s) {
        if (!m.crouching) return;
        float k = Mth.clamp(s.w[3] + s.w[4], 0f, 1f);
        m.body.y -= 3.2f * k; m.head.y -= 4.2f * k;
        m.rightArm.y -= 3.2f * k; m.leftArm.y -= 3.2f * k;
        m.rightLeg.y -= 0.2f * k; m.leftLeg.y -= 0.2f * k;
    }

    // ===== vida por cima das animacoes: respiracao (mais rapida e funda quando cansado) e movimentos involuntarios
    // (a cabeca vagueia, o tronco balanca, os bracos soltos oscilam) que nunca se repetem igual
    private static final float BREATH_CHEST = 0.025f, BREATH_CHEST_TIRED = 0.045f;   // peito enchendo (escala)
    private static final float BREATH_SHOULDER = 0.25f, BREATH_SHOULDER_TIRED = 0.45f; // ombros subindo (pixels)

    private static void alive(HumanoidModel<?> m, Player p, State s, float t, boolean rightFree, boolean leftFree) {
        float ex = s.exertion;
        // inspira mais rapido que expira (curva assimetrica), 0..1
        float b = 0.5f - 0.5f * Mth.cos(s.breath);
        b = b * b * (3 - 2 * b);
        float chest = (BREATH_CHEST + (BREATH_CHEST_TIRED - BREATH_CHEST) * ex) * b;
        m.body.zScale *= 1f + chest;
        m.body.yScale *= 1f + chest * 0.25f;
        float sh = (BREATH_SHOULDER + (BREATH_SHOULDER_TIRED - BREATH_SHOULDER) * ex) * b;
        m.rightArm.y -= sh; m.leftArm.y -= sh;
        m.head.y -= sh * 0.5f;
        m.head.xRot -= (0.6f + 1.4f * ex) * b * Mth.DEG_TO_RAD;
        // involuntarios: mais fortes parado, quase nada andando/correndo
        float idle = Mth.clamp(s.w[0] + s.w[3], 0f, 1f);
        float still = 0.25f + 0.75f * idle;
        float q = s.seed;
        float n1 = wander(t, q, 0.071f, 0.163f, 0.297f), n2 = wander(t, q + 7f, 0.089f, 0.211f, 0.383f);
        float n3 = wander(t, q + 13f, 0.053f, 0.137f, 0.241f), n4 = wander(t, q + 21f, 0.11f, 0.29f, 0.47f);
        m.head.yRot += 2.4f * still * n1 * Mth.DEG_TO_RAD;
        m.head.xRot += 1.1f * still * n2 * Mth.DEG_TO_RAD;
        m.body.zRot += 0.5f * still * n3 * Mth.DEG_TO_RAD;
        m.body.yRot += 0.7f * still * n1 * Mth.DEG_TO_RAD;
        if (rightFree) { m.rightArm.zRot += 0.9f * still * (n4 + ex * 0.6f * b) * Mth.DEG_TO_RAD; m.rightArm.xRot += 0.6f * still * n2 * Mth.DEG_TO_RAD; }
        if (leftFree) { m.leftArm.zRot -= 0.9f * still * (n3 + ex * 0.6f * b) * Mth.DEG_TO_RAD; m.leftArm.xRot += 0.6f * still * n1 * Mth.DEG_TO_RAD; }
    }

    // ruido suave (-1..1): tres ondas que nao batem entre si, entao o movimento nunca repete igual
    private static float wander(float t, float seed, float f1, float f2, float f3) {
        return (Mth.sin((t * f1 + seed * 0.37f) * Mth.TWO_PI) * 0.55f
                + Mth.sin((t * f2 + seed * 0.71f) * Mth.TWO_PI) * 0.3f
                + Mth.sin((t * f3 + seed * 0.13f) * Mth.TWO_PI) * 0.15f);
    }

    // braco: animacao so quando esta livre; segurando item parado, o jogo levanta um pouco o braco (igual ao jogo)
    private static boolean arm(HumanoidModel<?> m, BbAnims.Sample a, ModelPart arm, String bone, HumanoidModel.ArmPose pose,
                               boolean mir, float dx, boolean swinging) {
        if (swinging || (pose != HumanoidModel.ArmPose.EMPTY && pose != HumanoidModel.ArmPose.ITEM)) return false;
        BbAnims.part(arm, a, bone, 1f, mir, dx, 2, 0, false);
        if (pose == HumanoidModel.ArmPose.ITEM) arm.xRot = arm.xRot * 0.5f - Mth.PI / 10f;
        return true;
    }

    // corpo inteiro (root/tumble: sobe e desce, inclina) na hora de desenhar; golpes do Combat mandam por cima
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (MovementHooks.bodyBusy(p) || MovementHooks.stance(p)) return;
        BbAnims.Sample a = sample(p, e.getPartialTick());
        if (a == null) return;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        State s = STATES.get(p.getId());
        if (s != null && p.isCrouching()) ps.translate(0, 0.125f * Mth.clamp(s.w[3] + s.w[4], 0f, 1f), 0);   // desfaz o abaixar do jogo
        BbAnims.poseRoot(ps, a, 1f, p.getMainArm() == HumanoidArm.RIGHT, Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderPost(RenderPlayerEvent.Post e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }

    // camera de primeira pessoa: metade do balanco do corpo (pulo, aterrissagem, passada), sem a cabeca da animacao.
    // Respeita a opcao do jogo "Movimento da visao".
    public static MovementHooks.BodyFrame bodyFrame(Player p, float pt) {
        if (!Minecraft.getInstance().options.bobView().get()) return null;
        BbAnims.Sample a = sample(p, pt);
        if (a == null) return null;
        return new MovementHooks.BodyFrame(a, CAMERA_SHARE, p.getMainArm() == HumanoidArm.RIGHT, 0f, 0.3f);
    }

    // respiracao agora (0 = expirou, 1 = peito cheio), mais funda com cansaco: para a camera e as maos da primeira pessoa
    public static float breath(Player p) {
        State s = STATES.get(p.getId());
        if (s == null) return 0f;
        float b = 0.5f - 0.5f * Mth.cos(s.breath);
        return b * b * (3 - 2 * b) * (1f + 1.2f * s.exertion);
    }

    // tem animacao do Blockbench para este jogador agora? (a vida procedural fica de fora)
    public static boolean active(Player p) { return !anims().isEmpty() && !skip(p) && !MovementHooks.stance(p); }
}
