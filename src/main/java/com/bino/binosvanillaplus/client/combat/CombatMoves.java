package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.HeldToolModel;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.CombatMove;
import com.bino.binosvanillaplus.combat.MoveBroadcastPayload;
import com.bino.binosvanillaplus.combat.MovePayload;
import com.bino.binosvanillaplus.combat.MoveTiming;
import com.bino.binosvanillaplus.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Hack and slash (mod Combat), lado do cliente. So com ESPADA ou MACHADO (uma mao) na mao principal.
//
// CONTROLES
//  - esquerdo = golpe leve. O golpe tem o tempo da recarga da arma (estilo Better Combat): o dano sai NO IMPACTO da
//    animacao, nao no clique. Clicar durante um golpe guarda o proximo (encadeia sozinho). Correndo = investida; no
//    ar = golpe pulando. Se a mira nao estiver em ninguem, o golpe procura um monstro bem na frente (ajuda de mira).
//  - direito EM COMBATE = golpe pesado (servidor acerta em arco, empurra forte). No ar = pancada no chao; correndo =
//    investida pesada. Com escudo na outra mao, agachado + direito ainda defende.
//  - combo de ate 5 golpes misturando tudo; o 5o e' o FINAL. Contador do combo do lado da mira.
//  - em combate = bateu ou apanhou nos ultimos 6 s, ou tem monstro a ate 12 blocos.
//
// VISUAL
//  - primeira pessoa: mao/arma desenhadas por nos (com o braco da skin), seguindo as chaves de MoveAnims, com rastro
//    branco da lamina no trecho do golpe; a camera inclina com o tronco e da um "soco" no FOV nos golpes fortes.
//  - terceira pessoa (voce no F5 e os outros jogadores): braco aponta para a mao da animacao, a lamina gira junto,
//    tronco torce, pernas dao o passo, o corpo inclina/pula/rodopia (RenderPlayerEvent).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class CombatMoves {
    private CombatMoves() {}

    private static final long COMBAT_MS = 6000, COMBO_GAP_MS = 500, BLEND_MS = 60, TRAIL_MS = 110, EQUIP_MS = 260;

    static final class Active {
        final CombatMove move; final boolean heavy, axe; final long start; final int length; final MoveAnims.Key[] keys;
        boolean hitDone;
        Active(CombatMove move, boolean heavy, boolean axe, long start, int length) {
            this.move = move; this.heavy = heavy; this.axe = axe; this.start = start; this.length = length;
            this.keys = MoveAnims.keys(move, axe);
        }
        float u(long now) { return Mth.clamp((now - start) / (float) length, 0f, 1f); }
        boolean done(long now) { return now - start >= length; }
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static Boolean queued;            // proximo golpe guardado (true = pesado)
    private static int step, light;
    private static long lastMoveEnd, lastCombat, heavyReady;
    private static int hudStep; private static long hudAt; private static boolean hudFinal;
    private static long punchAt = -100000; private static float punchPower;
    private static ItemStack lastHeld = ItemStack.EMPTY; private static long equipAt = -100000;

    private record TrailPt(Vector3f base, Vector3f tip, long t) {}
    private static final Deque<TrailPt> TRAIL = new ArrayDeque<>();

    private static long now() { return System.currentTimeMillis(); }

    // ================================================================== entrada

    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        long now = now();
        Active cur = ACTIVE.get(p.getId());
        boolean busy = cur != null && !cur.done(now);
        boolean onBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        if (e.isAttack()) {
            if (onBlock && !busy) return;   // minerando: normal
            e.setCanceled(true);
            e.setSwingHand(false);
            request(p, false, cur, now);
        } else if (e.isUseItem()) {
            if (onBlock || !inCombat(p, now)) return;   // descascar tronco etc.
            if (p.isShiftKeyDown() && p.getOffhandItem().getItem() instanceof ShieldItem) return;   // defender
            e.setCanceled(true);
            e.setSwingHand(false);
            lastCombat = now;
            request(p, true, cur, now);
        }
    }

    private static void request(LocalPlayer p, boolean heavy, Active cur, long now) {
        if (cur == null || cur.done(now)) begin(p, heavy, now);
        else if (cur.u(now) >= 0.25f) queued = heavy;   // guarda o proximo (encadeia quando este acabar)
    }

    private static CombatMove pick(LocalPlayer p, boolean heavy, int next) {
        if (next >= CombatMove.MAX_COMBO) return CombatMove.FINISHER;
        if (!p.onGround() && !p.isInWater() && !p.onClimbable()) return CombatMove.JUMP;
        if (p.isSprinting()) return CombatMove.FORWARD;
        if (heavy) return CombatMove.HEAVY;
        return switch (light % 3) { case 0 -> CombatMove.LIGHT_1; case 1 -> CombatMove.LIGHT_2; default -> CombatMove.LIGHT_3; };
    }

    private static void begin(LocalPlayer p, boolean heavy, long now) {
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE) return;
        if (heavy && now < heavyReady) return;
        boolean axe = w == CombatEvents.Weapon.AXE;
        int next = now <= lastMoveEnd + COMBO_GAP_MS ? step + 1 : 1;
        if (next == 1) light = 0;
        CombatMove m = pick(p, heavy, next);
        if (m == CombatMove.LIGHT_1 || m == CombatMove.LIGHT_2 || m == CombatMove.LIGHT_3) light++;
        int len = MoveTiming.lengthMs(m, heavy, axe, p.getCurrentItemAttackStrengthDelay());
        ACTIVE.put(p.getId(), new Active(m, heavy, axe, now, len));
        lastMoveEnd = now + len;
        if (heavy) heavyReady = now + len - 80;
        hudStep = Math.min(next, CombatMove.MAX_COMBO); hudAt = now; hudFinal = m == CombatMove.FINISHER;
        step = m == CombatMove.FINISHER ? 0 : next;   // fechou o combo: recomeca
        // passo para dentro do golpe / investida
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() > 1e-4) {
            flat = flat.normalize();
            double push = m == CombatMove.FORWARD ? (heavy ? 0.65 : 0.4) : p.onGround() ? (heavy ? 0.12 : 0.06) : 0;
            p.setDeltaMovement(p.getDeltaMovement().add(flat.scale(push)));
        }
        if (m == CombatMove.JUMP && heavy) p.setDeltaMovement(p.getDeltaMovement().add(0, 0.15, 0));   // sobe um pouco e cai batendo
        PacketDistributor.sendToServer(new MovePayload((byte) m.ordinal(), heavy, (byte) next));
    }

    private static boolean inCombat(LocalPlayer p, long now) {
        if (p.hurtTime > 0) lastCombat = now;
        if (now - lastCombat < COMBAT_MS) return true;
        return !p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12),
                le -> le instanceof Enemy && le.isAlive()).isEmpty();
    }

    // a cada quadro: impacto do golpe, fim do golpe e o proximo guardado
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        long now = now();
        Active a = ACTIVE.get(p.getId());
        if (a == null) { queued = null; return; }
        if (!a.hitDone && a.u(now) >= MoveTiming.impact(a.move)) { a.hitDone = true; impact(mc, p, a); }
        boolean canChain = a.u(now) >= (a.heavy ? 0.92f : 0.86f);
        if (queued != null && canChain) { boolean h = queued; queued = null; begin(p, h, now); }
        else if (a.done(now)) ACTIVE.remove(p.getId());
    }

    private static void impact(Minecraft mc, LocalPlayer p, Active a) {
        if (a.heavy) {   // o servidor faz o dano do pesado; aqui so o peso
            HitFx.kick(a.move == CombatMove.JUMP ? 1.4f : 1.1f);
            punchAt = now(); punchPower = a.move == CombatMove.FINISHER || a.move == CombatMove.JUMP ? 1f : 0.7f;
            p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), a.axe ? ModSounds.AXE_SWING.get() : ModSounds.SWORD_SWING.get(),
                    SoundSource.PLAYERS, 1f, 0.72f, false);
            lastCombat = now();
            return;
        }
        if (a.move == CombatMove.FINISHER) { punchAt = now(); punchPower = 0.6f; }
        Entity target = mc.hitResult instanceof EntityHitResult eh ? eh.getEntity() : assist(p);
        if (target != null && mc.gameMode != null) {
            lastCombat = now();
            mc.gameMode.attack(p, target);   // o ataque normal do jogo (dano, critico, varrida, encantamentos)
        } else HitFx.swingAir(p);
    }

    // ajuda de mira: monstro bem na frente, dentro do alcance
    private static Entity assist(LocalPlayer p) {
        double reach = p.entityInteractionRange();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Entity best = null; double bestD = Double.MAX_VALUE;
        for (LivingEntity le : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach + 1),
                le -> le instanceof Enemy && le.isAlive() && le.isPickable())) {
            Vec3 c = le.getBoundingBox().getCenter();
            Vec3 to = c.subtract(eye);
            double d = to.length() - le.getBbWidth() * 0.5;
            if (d > reach || d >= bestD || to.normalize().dot(look) < 0.82 || !p.hasLineOfSight(le)) continue;
            best = le; bestD = d;
        }
        return best;
    }

    // outro jogador comecou um golpe
    public static void receive(MoveBroadcastPayload m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(m.entityId()) instanceof Player pl)) return;
        boolean axe = CombatEvents.weapon(pl.getMainHandItem()) == CombatEvents.Weapon.AXE;
        ACTIVE.put(m.entityId(), new Active(CombatMove.of(m.move()), m.heavy(), axe, now(), Math.max(100, m.lengthMs())));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { ACTIVE.clear(); PUSHED.clear(); TRAIL.clear(); step = 0; queued = null; }

    // ================================================================== pose

    // pose agora (null = sem golpe) e o peso dela (entra/sai suave)
    private static Active activeFor(Player p) {
        Active a = ACTIVE.get(p.getId());
        if (a == null) return null;
        if (a.done(now()) || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) {
            if (p != Minecraft.getInstance().player) ACTIVE.remove(p.getId());
            return null;
        }
        return a;
    }

    private static float[] pose(Active a, long now) {
        float[] v = MoveAnims.sample(a.keys, a.u(now));
        float in = Mth.clamp((now - a.start) / (float) BLEND_MS, 0f, 1f);
        in = in * in * (3 - 2 * in);
        for (int i = 0; i < MoveAnims.N; i++) if (i != MoveAnims.SP) v[i] = MoveAnims.REST[i] + (v[i] - MoveAnims.REST[i]) * in;
        return v;
    }

    // terceira pessoa entra/sai da pose do jogo base
    private static float tpWeight(Active a, long now) {
        float u = a.u(now);
        float w = Math.min(1f, u / 0.1f) * Math.min(1f, (1f - u) / 0.22f);
        w = Mth.clamp(w, 0f, 1f);
        return w * w * (3 - 2 * w);
    }

    // direcao da ponta (espaco da camera)
    private static Vector3f bladeDir(float ay, float ap) {
        return new Quaternionf().rotateY((float) Math.toRadians(-ay)).rotateX((float) Math.toRadians(-ap)).transform(new Vector3f(0, 1, 0));
    }

    // ------------------------------------------------------------------ primeira pessoa

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        ItemStack stack = e.getItemStack();
        if (CombatEvents.weapon(stack) == CombatEvents.Weapon.NONE || p.isUsingItem() && p.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            lastHeld = stack.copy();
            return;
        }
        e.setCanceled(true);
        long now = now();
        if (!ItemStack.isSameItem(stack, lastHeld)) equipAt = now;
        lastHeld = stack.copy();

        Active a = activeFor(p);
        float[] v = a != null ? pose(a, now) : MoveAnims.REST.clone();
        if (a == null) {
            float sw = e.getSwingProgress();   // minerando
            if (sw > 0) {
                float f = Mth.sin(Mth.sqrt(sw) * Mth.PI);
                v[MoveAnims.AP] += 60 * f; v[MoveAnims.HY] -= 0.10f * f; v[MoveAnims.HZ] -= 0.12f * f; v[MoveAnims.HX] -= 0.08f * f;
            }
            v[MoveAnims.HY] += Mth.sin(now / 650f) * 0.006f;   // respiracao
        }
        float eq = 1f - Mth.clamp((now - equipAt) / (float) EQUIP_MS, 0f, 1f);
        v[MoveAnims.HY] -= 0.6f * eq * eq;

        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        int light = e.getPackedLight();
        Vector3f hand = new Vector3f(v[MoveAnims.HX] * side, v[MoveAnims.HY], v[MoveAnims.HZ]);

        // braco (skin) do ombro ate a mao
        Vec3 shoulder = new Vec3(0.40 * side, -0.86, 0.12);
        LapidaryHands.arm(ps, buf, Vec3.ZERO, new Vec3(hand.x, hand.y, hand.z), shoulder, side > 0, light, new Vec3(0, 0, -1), p, 0.85f);

        ps.pushPose();
        ps.translate(hand.x, hand.y, hand.z);
        ps.mulPose(Axis.YP.rotationDegrees(-v[MoveAnims.AY] * side));
        ps.mulPose(Axis.XP.rotationDegrees(-v[MoveAnims.AP]));
        ps.mulPose(Axis.YP.rotationDegrees(v[MoveAnims.ER] * side));

        // rastro: guarda base e ponta da lamina enquanto golpeia
        if (a != null && MoveAnims.striking(a.keys, a.u(now))) {
            Matrix4f m = ps.last().pose();
            float len = a.axe ? 0.72f : 0.84f;
            TRAIL.addLast(new TrailPt(m.transformPosition(new Vector3f(0, len * 0.3f, 0)), m.transformPosition(new Vector3f(0, len, 0)), now));
        }
        renderWeapon(mc, p, stack, ps, buf, light);
        ps.popPose();
        renderTrail(buf, now, a != null && a.heavy);
    }

    // a arma com o cabo na origem e a ponta para +Y
    private static void renderWeapon(Minecraft mc, LocalPlayer p, ItemStack stack, PoseStack ps, MultiBufferSource buf, int light) {
        BakedModel model = mc.getItemRenderer().getModel(stack, p.level(), p, p.getId());
        ps.pushPose();
        if (model instanceof HeldToolModel h) {   // modelo 3D do mod (em pe, cabo embaixo)
            ps.scale(0.55f, 0.55f, 0.55f);
            ps.translate(0, 0.36f, 0);
            if (CombatEvents.weapon(stack) == CombatEvents.Weapon.SWORD) ps.mulPose(Axis.YP.rotationDegrees(90));   // lado chato da lamina para a camera
            mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, light, OverlayTexture.NO_OVERLAY, h.solid());
        } else {                                   // icone 2D (diagonal: cabo embaixo-esquerda, ponta em cima-direita)
            ps.scale(0.72f, 0.72f, 0.72f);
            ps.translate(0, 0.42f, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(45));
            mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, light, OverlayTexture.NO_OVERLAY, model);
        }
        ps.popPose();
    }

    private static void renderTrail(MultiBufferSource buf, long now, boolean heavy) {
        for (Iterator<TrailPt> it = TRAIL.iterator(); it.hasNext(); ) if (now - it.next().t() > TRAIL_MS) it.remove();
        if (TRAIL.size() < 2) return;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        int g = heavy ? 225 : 245, b = heavy ? 190 : 255;
        TrailPt prev = null;
        for (TrailPt pt : TRAIL) {
            if (prev != null && prev.tip().distanceSquared(pt.tip()) > 1e-6) {
                int a0 = (int) (150 * (1 - (now - prev.t()) / (float) TRAIL_MS)), a1 = (int) (150 * (1 - (now - pt.t()) / (float) TRAIL_MS));
                quad(vc, prev.base(), prev.tip(), pt.tip(), pt.base(), a0, a1, g, b);
            }
            prev = pt;
        }
    }

    private static void quad(VertexConsumer vc, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1, int g, int b) {
        // dos dois lados (sem depender do sentido do golpe); a base some, a ponta brilha
        vc.addVertex(b0.x, b0.y, b0.z).setColor(255, g, b, 0);
        vc.addVertex(t0.x, t0.y, t0.z).setColor(255, g, b, a0);
        vc.addVertex(t1.x, t1.y, t1.z).setColor(255, g, b, a1);
        vc.addVertex(b1.x, b1.y, b1.z).setColor(255, g, b, 0);
        vc.addVertex(b1.x, b1.y, b1.z).setColor(255, g, b, 0);
        vc.addVertex(t1.x, t1.y, t1.z).setColor(255, g, b, a1);
        vc.addVertex(t0.x, t0.y, t0.z).setColor(255, g, b, a0);
        vc.addVertex(b0.x, b0.y, b0.z).setColor(255, g, b, 0);
    }

    // camera acompanha o tronco (inclina no corte, abaixa na pancada)
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        Active a = activeFor(mc.player);
        if (a == null) return;
        float[] v = pose(a, now());
        float side = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        e.setRoll(e.getRoll() - v[MoveAnims.TW] * 0.07f * side);
        e.setPitch(e.getPitch() + v[MoveAnims.LN] * 0.06f);
        e.setYaw(e.getYaw() + v[MoveAnims.TW] * 0.04f * side);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        long dt = now() - punchAt;
        if (dt > 260) return;
        float k = 1f - dt / 260f;
        e.setFOV(e.getFOV() * (1 + 0.05f * punchPower * k * k));
    }

    // ------------------------------------------------------------------ contador do combo (do lado da mira)

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || hudStep <= 0) return;
        long now = now(), end = Math.max(lastMoveEnd + COMBO_GAP_MS, hudAt + 600);
        if (now > end + 350) return;
        float a = now > end ? 1f - (now - end) / 350f : 1f;
        GuiGraphics g = e.getGuiGraphics();
        int cx = g.guiWidth() / 2 + 14, cy = g.guiHeight() / 2 + 8;
        float pop = 1f + 0.45f * Math.max(0f, 1f - (now - hudAt) / 160f);
        int col = hudFinal ? 0xFFD54A : 0xFFFFFF;
        int alpha = (int) (a * 255) << 24;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(pop, pop, 1);
        String s = hudFinal ? "x" + hudStep + "!" : "x" + hudStep;
        g.drawString(mc.font, s, 0, -4, alpha | col, true);
        g.pose().popPose();
        for (int i = 0; i < CombatMove.MAX_COMBO; i++) {
            int x = cx + i * 6, y = cy + 7;
            g.fill(x, y, x + 4, y + 2, i < hudStep ? (alpha | col) : ((int) (a * 90) << 24) | 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ terceira pessoa

    // braco, lamina, tronco e pernas (chamado no fim do setupAnim do HumanoidModel)
    public static void posePlayer(HumanoidModel<?> m, Player p) {
        Active a = activeFor(p);
        if (a == null) return;
        long now = now();
        float w = tpWeight(a, now);
        if (w <= 0.001f) return;
        float[] v = pose(a, now);
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1f : -1f;
        ModelPart main = right ? m.rightArm : m.leftArm, other = right ? m.leftArm : m.rightArm;

        // direcao do braco: do ombro ate a mao da animacao (espaco da camera -> espaco do modelo: x e y invertidos)
        Vector3f d = new Vector3f(v[MoveAnims.HX] * side - 0.30f * side, v[MoveAnims.HY] + 0.45f, v[MoveAnims.HZ]);
        Vector3f arm = new Vector3f(-d.x, -d.y, d.z).normalize();
        Vector3f bd = bladeDir(v[MoveAnims.AY] * side, v[MoveAnims.AP]);
        Vector3f blade = new Vector3f(-bd.x, -bd.y, bd.z);
        Quaternionf q = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), arm);
        Vector3f s0 = q.transform(new Vector3f(0, 0, -1));   // para onde a arma aponta com essa pose
        Vector3f t = new Vector3f(blade).sub(new Vector3f(arm).mul(arm.dot(blade)));
        if (t.lengthSquared() > 1e-5f) {
            t.normalize();
            float roll = (float) Math.atan2(new Vector3f(s0).cross(t).dot(arm), s0.dot(t));
            q = new Quaternionf().rotationAxis(roll, arm).mul(q);
        }
        Vector3f eu = q.getEulerAnglesZYX(new Vector3f());

        float tw = (float) Math.toRadians(v[MoveAnims.TW]) * side;
        m.body.yRot = Mth.lerp(w, m.body.yRot, tw);
        float by = m.body.yRot;
        m.rightArm.z = Mth.sin(by) * 5f; m.rightArm.x = -Mth.cos(by) * 5f;
        m.leftArm.z = -Mth.sin(by) * 5f; m.leftArm.x = Mth.cos(by) * 5f;

        main.xRot = Mth.lerp(w, main.xRot, eu.x);
        main.yRot = Mth.lerp(w, main.yRot, eu.y);
        main.zRot = Mth.lerp(w, main.zRot, eu.z);
        // a outra mao equilibra (abre para o lado e acompanha o tronco)
        other.xRot = Mth.lerp(w, other.xRot, -0.35f - (float) Math.toRadians(v[MoveAnims.LN]) * 0.6f);
        other.yRot = Mth.lerp(w, other.yRot, by);
        other.zRot = Mth.lerp(w, other.zRot, -0.45f * side);
        m.head.yRot -= by * 0.5f * w;   // a cabeca segura o olhar enquanto o tronco torce

        if (p.onGround()) {
            ModelPart front = right ? m.rightLeg : m.leftLeg, back = right ? m.leftLeg : m.rightLeg;
            float st = v[MoveAnims.ST];
            front.xRot = Mth.lerp(w, front.xRot, -st * 0.75f);
            back.xRot = Mth.lerp(w, back.xRot, st * 0.55f);
        }
    }

    // corpo inteiro: inclinar, pulinho e rodopio (gira em volta dos pes)
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        Active a = activeFor(p);
        if (a == null) return;
        long now = now();
        float[] v = pose(a, now);
        float w = tpWeight(a, now);
        float yaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        ps.translate(0, v[MoveAnims.HOP] * w, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.YP.rotationDegrees(-(v[MoveAnims.SP] % 360f)));
        ps.mulPose(Axis.XP.rotationDegrees(v[MoveAnims.LN] * 0.5f * w));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }
}
