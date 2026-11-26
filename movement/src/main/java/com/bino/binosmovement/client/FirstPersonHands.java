package com.bino.binosmovement.client;

import com.bino.binosmovement.BinosMovement;
import com.bino.binosmovement.MovementConfig;
import com.bino.binosmovement.client.anim.BbAnims;
import com.bino.binosmovement.client.anim.Locomotion;
import com.bino.binosmovement.mixin.client.ItemInHandRendererInvoker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

// Maos da primeira pessoa vivas: seguem o corpo das animacoes do dia a dia (o braco balanca para a frente e para
// tras andando/correndo, sobem no pulo, descem na aterrissagem, sobem e descem com a respiracao) e a mao livre
// (a esquerda, sem item) tambem aparece, nao so a direita.
// O desenho continua o do jogo (itens, comer, arco, picareta...): aqui so muda onde a mao fica.
// Golpes do Combat e cenas (bau, bancada...) desenham as maos deles e cancelam isto.
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonHands {
    private FirstPersonHands() {}

    private static final float SWING_FWD = 0.16f;    // braco a frente/atras (blocos por radiano de balanco)
    private static final float SWING_UP = 0.07f;     // e sobe um pouco quando vai a frente
    private static final float SWING_TILT = 0.25f;   // e a mao gira junto (fracao do balanco)
    private static final float OPEN_SIDE = 0.05f;    // braco abrindo para o lado
    private static final float BODY_SHARE = 0.35f;   // sobe/desce do corpo (pulo, aterrissagem, passada)
    private static final float BREATH = 0.010f;      // respiracao (blocos)

    // quanto a mao deste lado sai do lugar agora: {x (direita), y (cima), z (frente negativa), giro x (graus)}
    public static float[] handOffset(Player p, HumanoidArm side, float pt) {
        float[] out = new float[4];
        BbAnims.Sample a = Locomotion.sample(p, pt);
        float b = Locomotion.breath(p);
        out[1] += BREATH * b;
        if (a == null) return out;
        boolean main = side == p.getMainArm();
        float[] r = a.rot(main ? "right_arm" : "left_arm");
        float swing = r[0] * Mth.DEG_TO_RAD;                       // + = braco a frente
        float open = Math.abs(r[2]) * Mth.DEG_TO_RAD;              // abrindo para fora
        float sign = side == HumanoidArm.RIGHT ? 1f : -1f;
        out[2] -= Mth.sin(swing) * SWING_FWD;
        out[1] += (1f - Mth.cos(swing)) * SWING_UP * Math.signum(swing);
        out[3] += r[0] * SWING_TILT;
        out[0] += sign * Mth.sin(open) * OPEN_SIDE;
        float[] rp = a.pos("root");
        out[1] += rp[1] * BbAnims.px() * BODY_SHARE;
        return out;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHand(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !MovementConfig.firstPersonBody() || !mc.options.getCameraType().isFirstPerson()) return;
        if (p.isUsingItem() && p.getUsedItemHand() == e.getHand()) return;   // comendo, arco, escudo: o jogo cuida
        HumanoidArm side = e.getHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
        float pt = e.getPartialTick();
        float[] o = handOffset(p, side, pt);
        var renderer = (ItemInHandRendererInvoker) mc.getEntityRenderDispatcher().getItemInHandRenderer();
        PoseStack ps = e.getPoseStack();
        boolean emptyOff = e.getHand() == InteractionHand.OFF_HAND && e.getItemStack().isEmpty();
        if (emptyOff && (p.isUsingItem() || !p.getMainHandItem().isEmpty() && twoHanded(p))) return;
        e.setCanceled(true);
        ps.pushPose();
        ps.translate(o[0], o[1], o[2]);
        ps.mulPose(Axis.XP.rotationDegrees(-o[3]));
        if (emptyOff) renderer.binosmovement$renderPlayerArm(ps, e.getMultiBufferSource(), e.getPackedLight(), 0f, 0f, side);   // a mao livre tambem
        else renderer.binosmovement$renderArmWithItem(p, pt, e.getInterpolatedPitch(), e.getHand(), e.getSwingProgress(),
                e.getItemStack(), e.getEquipProgress(), ps, e.getMultiBufferSource(), e.getPackedLight());
        ps.popPose();
    }

    // item que ocupa as duas maos (arco, besta, tridente): a outra mao nao aparece
    private static boolean twoHanded(Player p) {
        var it = p.getMainHandItem().getItem();
        return it instanceof net.minecraft.world.item.BowItem || it instanceof net.minecraft.world.item.CrossbowItem
                || it instanceof net.minecraft.world.item.TridentItem;
    }
}
