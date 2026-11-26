package com.bino.binosmovement.client;

import com.bino.binosmovement.BinosMovement;
import com.bino.binosmovement.MovementConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

// O proprio corpo em primeira pessoa: olhando para baixo voce ve o tronco e as pernas (andando, pulando, no golpe).
// A cabeca e o chapeu nao aparecem (a camera fica dentro da cabeca); os itens nas maos sao os da primeira pessoa. O corpo vai um pouco para tras quando voce olha para baixo, para a camera nao
// entrar no peito.
// Quem faz o jogo desenhar o jogador em primeira pessoa: LevelRendererBodyMixin. Capacete e itens: os mixins
// HumanoidArmorLayerBodyMixin e ItemInHandLayerBodyMixin olham para rendering().
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonBody {
    private FirstPersonBody() {}

    private static boolean rendering;
    private static boolean pushed;
    // Modelo: o First-person Model (tr7zw). O corpo inteiro aparece (tronco, bracos, pernas, armadura), menos a cabeca;
    // recua um pouco para a camera nao ficar dentro do peito; maos vazias sao os bracos do proprio corpo (nada de
    // maozinha do jogo), e a mao com item continua a do jogo (itens, minerar, comer, arco), sem braco duplicado.
    private static final float BACK = 0.16f, BACK_LOOKING_DOWN = 0.22f, BACK_CROUCH = 0.20f;   // blocos para tras

    // quanto o corpo recua (blocos), para longe de onde voce olha: a camera nunca fica dentro do peito
    public static float back(Player p, float pt) {
        float pitch = Mth.clamp(p.getViewXRot(pt) / 90f, 0f, 1f);
        return BACK + BACK_LOOKING_DOWN * pitch + (p.isCrouching() ? BACK_CROUCH : 0f);
    }

    // o tronco sempre aparece (como no First-person Model)
    public static boolean torsoHidden(Player p, float pt) { return false; }
    public static boolean hidingTorso() { return false; }

    // a mao deste lado e' desenhada pelo jogo (tem item)? entao o braco do corpo nao aparece (sem braco duplicado)
    public static boolean vanillaHand(Player p, net.minecraft.world.entity.HumanoidArm side) {
        boolean main = side == p.getMainArm();
        return !(main ? p.getMainHandItem() : p.getOffhandItem()).isEmpty();
    }

    private static boolean rightShown, leftShown;
    // braco do corpo aparecendo agora (para a manga da armadura do peito)
    public static boolean armShown(boolean rightArm) { return rendering && (rightArm ? rightShown : leftShown); }

    // o jogo deve desenhar o jogador local agora, mesmo em primeira pessoa?
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        if (!MovementConfig.firstPersonBody() || mc.player == null || mc.getCameraEntity() != mc.player) return false;
        if (!mc.options.getCameraType().isFirstPerson()) return false;
        Player p = mc.player;
        return !p.isSpectator() && !p.isInvisible() && !p.isSleeping() && !p.isFallFlying() && !p.isVisuallySwimming()
                && !MovementHooks.fpBodyElsewhereFn.test(p);
    }

    // desenhando o corpo da primeira pessoa neste instante (para esconder cabeca, capacete e itens)
    public static boolean rendering() { return rendering; }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderPlayerEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getEntity() != mc.player || !active() || e.isCanceled()) return;
        rendering = true;
        PlayerModel<?> m = e.getRenderer().getModel();
        m.head.visible = false; m.hat.visible = false;
        Player p = e.getEntity();
        rightShown = !vanillaHand(p, net.minecraft.world.entity.HumanoidArm.RIGHT);
        leftShown = !vanillaHand(p, net.minecraft.world.entity.HumanoidArm.LEFT);
        if (!rightShown) { m.rightArm.visible = false; m.rightSleeve.visible = false; }
        if (!leftShown) { m.leftArm.visible = false; m.leftSleeve.visible = false; }
        float pt = e.getPartialTick();
        float back = back(p, pt);
        float yaw = p.getViewYRot(pt) * Mth.DEG_TO_RAD;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        pushed = true;
        ps.translate(Mth.sin(yaw) * back, 0, -Mth.cos(yaw) * back);   // para tras de onde voce olha
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPost(RenderPlayerEvent.Post e) {
        if (e.getEntity() != Minecraft.getInstance().player) return;
        if (pushed) { e.getPoseStack().popPose(); pushed = false; }
        if (rendering) {
            rendering = false;
            PlayerModel<?> m = e.getRenderer().getModel();
            m.head.visible = true; m.hat.visible = true;
            m.body.visible = true; m.jacket.visible = true;
            m.rightArm.visible = true; m.leftArm.visible = true;
            m.rightSleeve.visible = true; m.leftSleeve.visible = true;
        }
    }
}
