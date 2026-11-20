package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.CombatMove;
import com.bino.binosvanillaplus.combat.MoveBroadcastPayload;
import com.bino.binosvanillaplus.combat.MovePayload;
import com.bino.binosvanillaplus.combat.MoveTiming;
import com.bino.binosvanillaplus.mixin.client.ItemInHandRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

// Hack and slash (mod Combat), lado do cliente. So com ESPADA ou MACHADO na mao:
//  - botao esquerdo = golpe leve (o ataque normal do jogo, agora com animacao propria). Correndo vira golpe para a
//    frente (investida/estocada); no ar vira golpe pulando.
//  - botao direito EM COMBATE = golpe pesado (mais lento, acerta em arco, empurra). No ar vira pancada no chao;
//    correndo vira investida pesada. (com escudo na outra mao: agachado + direito ainda defende)
//  - combo de ate 5 golpes misturando tudo: cada golpe dentro da janela soma 1; o 5o vira o FINAL do combo.
//  - em combate = bateu ou apanhou nos ultimos 6 s, ou tem monstro a ate 12 blocos.
// As animacoes (diferentes para espada e machado) estao em MoveAnims; aqui elas viram pose da mao em primeira
// pessoa (RenderHandEvent) e do braco/corpo em terceira pessoa (HumanoidModelCombatMixin), inclusive dos outros jogadores.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class CombatMoves {
    private CombatMoves() {}

    private static final long COMBAT_MS = 6000, COMBO_GAP_MS = 450, BLEND_MS = 70;

    record Active(CombatMove move, boolean heavy, boolean axe, long start, int length) {
        float u(long now) { return Math.min(1f, (now - start) / (float) length); }
        boolean done(long now) { return now - start >= length; }
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static int step;              // golpes no combo atual (0 = nenhum)
    private static int light;             // alterna LIGHT_1/2/3
    private static long lastMoveEnd;      // quando o ultimo golpe termina
    private static long lastCombat;       // ultimo bate/apanha

    // ---------- entrada ----------

    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || e.getHand() != InteractionHand.MAIN_HAND) return;
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE) return;
        boolean axe = w == CombatEvents.Weapon.AXE;
        long now = System.currentTimeMillis();
        Active cur = ACTIVE.get(p.getId());
        boolean busyHeavy = cur != null && cur.heavy() && !cur.done(now);

        if (e.isAttack()) {
            if (busyHeavy) { e.setCanceled(true); e.setSwingHand(false); return; }   // no meio do golpe pesado
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;   // minerando: normal
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) lastCombat = now;
            start(p, pick(p, false), false, axe, now);
        } else if (e.isUseItem()) {
            if (!inCombat(p, now)) return;
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;   // descascar tronco etc.
            if (p.isShiftKeyDown() && p.getOffhandItem().getItem() instanceof ShieldItem) return;   // defender com escudo
            e.setCanceled(true);
            e.setSwingHand(false);
            if (busyHeavy) return;
            lastCombat = now;
            start(p, pick(p, true), true, axe, now);
        }
    }

    private static CombatMove pick(LocalPlayer p, boolean heavy) {
        long now = System.currentTimeMillis();
        int next = now <= lastMoveEnd + COMBO_GAP_MS ? step + 1 : 1;
        if (next >= CombatMove.MAX_COMBO) return CombatMove.FINISHER;
        if (!p.onGround() && !p.isInWater() && !p.onClimbable()) return CombatMove.JUMP;
        if (p.isSprinting()) return CombatMove.FORWARD;
        if (heavy) return CombatMove.HEAVY;
        return switch (light % 3) { case 0 -> CombatMove.LIGHT_1; case 1 -> CombatMove.LIGHT_2; default -> CombatMove.LIGHT_3; };
    }

    private static void start(LocalPlayer p, CombatMove m, boolean heavy, boolean axe, long now) {
        step = now <= lastMoveEnd + COMBO_GAP_MS ? step + 1 : 1;
        if (m == CombatMove.FINISHER || step >= CombatMove.MAX_COMBO) step = 0;   // combo fechado: recomeca
        if (m == CombatMove.LIGHT_1 || m == CombatMove.LIGHT_2 || m == CombatMove.LIGHT_3) light++;
        else if (step <= 1) light = 0;
        int len = MoveTiming.lengthMs(m, heavy, axe);
        ACTIVE.put(p.getId(), new Active(m, heavy, axe, now, len));
        lastMoveEnd = now + len;
        // investida: um passo para a frente
        if (m == CombatMove.FORWARD) {
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale(heavy ? 0.6 : 0.35);
            p.setDeltaMovement(p.getDeltaMovement().add(flat));
        }
        if (m == CombatMove.JUMP && heavy) p.setDeltaMovement(p.getDeltaMovement().add(0, -0.25, 0));   // desce para a pancada
        PacketDistributor.sendToServer(new MovePayload((byte) m.ordinal(), heavy, (byte) step));
    }

    private static boolean inCombat(LocalPlayer p, long now) {
        if (p.hurtTime > 0) lastCombat = now;
        if (now - lastCombat < COMBAT_MS) return true;
        return !p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(12),
                le -> le instanceof Enemy && le.isAlive()).isEmpty();
    }

    // outro jogador comecou um golpe (para ver em terceira pessoa)
    public static void receive(MoveBroadcastPayload m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var ent = mc.level.getEntity(m.entityId());
        if (!(ent instanceof Player pl)) return;
        boolean axe = CombatEvents.weapon(pl.getMainHandItem()) == CombatEvents.Weapon.AXE;
        CombatMove mv = CombatMove.of(m.move());
        ACTIVE.put(m.entityId(), new Active(mv, m.heavy(), axe, System.currentTimeMillis(), MoveTiming.lengthMs(mv, m.heavy(), axe)));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { ACTIVE.clear(); step = 0; }

    // ---------- pose ----------

    // valores da animacao agora (null = sem golpe): {sobe, varre, gira, empurra, levanta}
    private static float[] now(Player p) {
        Active a = ACTIVE.get(p.getId());
        if (a == null) return null;
        long t = System.currentTimeMillis();
        if (a.done(t) || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) { ACTIVE.remove(p.getId()); return null; }
        float[] v = MoveAnims.sample(MoveAnims.keys(a.move(), a.axe()), a.u(t));
        float in = Math.min(1f, (t - a.start()) / (float) BLEND_MS);   // entra suave a partir da pose parada
        in = in * in * (3 - 2 * in);
        for (int i = 0; i < v.length; i++) v[i] *= in;
        return v;
    }

    // primeira pessoa: a mao da arma segue a curva do golpe (gira em volta do ombro; o "gira" inclina na mao)
    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (e.isCanceled() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        float[] v = now(p);
        if (v == null) return;
        e.setCanceled(true);
        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(side * 0.35f, -0.35f, 0.1f);            // ombro
        ps.mulPose(Axis.YP.rotationDegrees(-v[1] * side));
        ps.mulPose(Axis.XP.rotationDegrees(v[0]));
        ps.translate(-side * 0.35f, 0.35f, -0.1f);
        ps.translate(0, v[4], -v[3]);
        ps.translate(side * 0.56f, -0.52f, -0.72f);          // mao (onde o jogo desenha o item)
        ps.mulPose(Axis.ZP.rotationDegrees(v[2] * side));
        ps.translate(-side * 0.56f, 0.52f, 0.72f);
        ((ItemInHandRendererAccessor) mc.getEntityRenderDispatcher().getItemInHandRenderer()).binosvanillaplus$renderArmWithItem(
                p, e.getPartialTick(), e.getInterpolatedPitch(), InteractionHand.MAIN_HAND, 0f, e.getItemStack(), 0f,
                ps, e.getMultiBufferSource(), e.getPackedLight());
        ps.popPose();
    }

    // terceira pessoa: braco da arma (e o outro, no machado a duas maos) e um giro do tronco
    public static void posePlayer(HumanoidModel<?> m, Player p) {
        float[] v = now(p);
        if (v == null) return;
        Active a = ACTIVE.get(p.getId());
        if (a == null) return;
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1f : -1f;
        ModelPart main = right ? m.rightArm : m.leftArm, other = right ? m.leftArm : m.rightArm;
        float up = (float) Math.toRadians(v[0]), sweep = (float) Math.toRadians(v[1]), roll = (float) Math.toRadians(v[2]);
        main.xRot = -1.45f - up * 1.1f - v[3] * 0.8f;
        main.yRot = -sweep * 0.8f * side;
        main.zRot = roll * 0.3f * side;
        m.body.yRot = -sweep * 0.45f * side;
        m.head.yRot += m.body.yRot * 0.3f;
        if (MoveAnims.twoHanded(a.move(), a.axe())) {
            other.xRot = main.xRot;
            other.yRot = main.yRot + 0.55f * side;   // a outra mao fecha no cabo
            other.zRot = -main.zRot;
        }
        // os bracos seguem o tronco quando ele gira
        float bodyTurn = m.body.yRot;
        main.yRot += bodyTurn;
        if (!MoveAnims.twoHanded(a.move(), a.axe())) other.yRot += bodyTurn;
    }
}
