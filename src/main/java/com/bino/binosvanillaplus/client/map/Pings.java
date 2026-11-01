package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.PetInteractables;
import com.bino.binosvanillaplus.network.PetActivatePayload;
import com.bino.binosvanillaplus.network.PingBroadcastPayload;
import com.bino.binosvanillaplus.network.PingPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Atalhos com Alt:
//  - Alt + botao esquerdo: PING no bloco que voce esta olhando (ate 256 blocos). Aparece por 5 s no minimapa, no
//    mapa-mundi e no mundo (para todos os jogadores da mesma dimensao, cada um com a sua cor).
//  - Alt + botao direito em botao/alavanca/porta/alcapao/portao/sino/bloco musical/repetidor/comparador/sensor de luz
//    (ate 24 blocos): o pet (allay) vai ate la e aciona. Blocos com menu (fornalha, bau...) nao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Pings {
    private Pings() {}

    public record Ping(BlockPos pos, String name, int color, long at) {}

    static final long LIFE_MS = 5000;
    private static final List<Ping> LIST = new ArrayList<>();
    private static long lastSend = 0;

    public static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pings");

    public static void receive(PingBroadcastPayload p) {
        LIST.removeIf(q -> q.name().equals(p.name()) && q.pos().equals(p.pos()));
        LIST.add(new Ping(p.pos(), p.name(), p.color(), Util.getMillis()));
        while (LIST.size() > 16) LIST.remove(0);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.7f, 0.45f));
    }

    static List<Ping> active() {
        long now = Util.getMillis();
        LIST.removeIf(p -> now - p.at() > LIFE_MS);
        return LIST;
    }

    // 0..1 do tempo de vida
    static float age(Ping p) { return Math.min(1f, (Util.getMillis() - p.at()) / (float) LIFE_MS); }

    private static boolean altDown(Minecraft mc) {
        long w = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !altDown(mc)) return;
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (event.isAttack()) {
            event.setCanceled(true);       // Alt + esquerdo nunca quebra/ataca
            event.setSwingHand(false);
            long now = Util.getMillis();
            if (now - lastSend < 300) return;
            lastSend = now;
            HitResult hr = mc.player.pick(256.0, pt, false);
            if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) {
                PacketDistributor.sendToServer(new PingPayload(bh.getBlockPos()));
            }
        } else if (event.isUseItem()) {
            HitResult hr = mc.player.pick(24.0, pt, false);
            if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK
                    && PetInteractables.allowed(mc.level.getBlockState(bh.getBlockPos()))) {
                event.setCanceled(true);
                event.setSwingHand(false);
                PacketDistributor.sendToServer(new PetActivatePayload(bh.getBlockPos(), bh.getDirection().get3DDataValue()));
            }
        }
    }

    // ---------------------------------------------------------------- desenho

    // marcador: losango na cor do jogador + anel que se expande; some no ultimo segundo
    static void drawMarker(GuiGraphics g, float x, float y, int color, float age, float scale) {
        float fade = Math.min(1f, (1f - age) * 5f);
        if (fade <= 0.01f) return;
        int a = (int) (255 * fade) << 24;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        // anel pulsando (2 por segundo)
        float ph = (age * LIFE_MS / 500f) % 1f;
        float r = 3f + 8f * ph;
        int ra = (int) (200 * fade * (1f - ph)) << 24;
        for (int i = 0; i < 20; i++) {
            double t = i / 20.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(t) * r), py = (int) Math.round(Math.sin(t) * r);
            g.fill(px, py, px + 1, py + 1, ra | (color & 0xFFFFFF));
        }
        // losango (quadrado girado 45 graus) com contorno preto
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45f));
        g.fill(-3, -3, 4, 4, a);
        g.fill(-2, -2, 3, 3, a | (color & 0xFFFFFF));
        g.pose().popPose();
    }

    // no mundo: losango + nome e distancia, na direcao do bloco
    public static void renderHud(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        List<Ping> list = active();
        if (list.isEmpty()) return;
        float ts = MinimapHud.smallScale(mc);
        for (Ping p : list) {
            float[] s = WaypointBeacons.toScreen(p.pos().getX() + 0.5, p.pos().getY() + 1.0, p.pos().getZ() + 0.5, g.guiWidth(), g.guiHeight());
            if (s == null) continue;
            float age = age(p);
            drawMarker(g, s[0], s[1], p.color(), age, 1.3f);
            int dist = (int) Math.sqrt(mc.player.distanceToSqr(p.pos().getX() + 0.5, p.pos().getY(), p.pos().getZ() + 0.5));
            String label = p.name() + " (" + dist + " m)";
            float tw = mc.font.width(label) * ts;
            int alpha = (int) (255 * Math.min(1f, (1f - age) * 5f));
            if (alpha < 8) continue;
            com.bino.binosvanillaplus.client.ui.BinoUi.text(g, mc.font, label, s[0] - tw / 2f, s[1] + 9, ts, (alpha << 24) | 0xFFFFFF, true);
        }
    }
}
