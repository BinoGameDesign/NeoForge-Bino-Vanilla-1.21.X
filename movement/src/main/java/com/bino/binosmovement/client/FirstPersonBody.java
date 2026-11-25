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
// A cabeca, o chapeu, os bracos e o que esta nas maos nao aparecem (as maos continuam as da primeira pessoa e a
// camera fica dentro da cabeca). O corpo vai um pouco para tras quando voce olha para baixo, para a camera nao
// entrar no peito.
// Quem faz o jogo desenhar o jogador em primeira pessoa: LevelRendererBodyMixin. Capacete e itens: os mixins
// HumanoidArmorLayerBodyMixin e ItemInHandLayerBodyMixin olham para rendering().
@EventBusSubscriber(modid = BinosMovement.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonBody {
    private FirstPersonBody() {}

    private static boolean rendering;
    private static boolean pushed;
    private static final float BACK = 0.22f, BACK_LOOKING_DOWN = 0.30f, BACK_CROUCH = 0.22f;   // blocos para tras
    private static final float TORSO_HIDE_PITCH = 45f;   // olhando mais para baixo que isso o tronco some (so pernas)

    // quanto o corpo recua (blocos), para longe de onde voce olha: a camera nunca fica dentro do peito
    public static float back(Player p, float pt) {
        float pitch = Mth.clamp(p.getViewXRot(pt) / 90f, 0f, 1f);
        return BACK + BACK_LOOKING_DOWN * pitch + (p.isCrouching() ? BACK_CROUCH : 0f);
    }

    // olhando bem para baixo: sem tronco (dava para ver o pescoco e o topo do peito); ficam as pernas e os pes
    public static boolean torsoHidden(Player p, float pt) { return p.getViewXRot(pt) > TORSO_HIDE_PITCH; }

    private static boolean hideTorso;
    public static boolean hidingTorso() { return rendering && hideTorso; }

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
        m.rightArm.visible = false; m.leftArm.visible = false;
        m.rightSleeve.visible = false; m.leftSleeve.visible = false;
        float pt = e.getPartialTick();
        Player p = e.getEntity();
        float back = back(p, pt);
        hideTorso = torsoHidden(p, pt);
        if (hideTorso) { m.body.visible = false; m.jacket.visible = false; }
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
            hideTorso = false;
            m.rightArm.visible = true; m.leftArm.visible = true;
            m.rightSleeve.visible = true; m.leftSleeve.visible = true;
        }
    }
}
