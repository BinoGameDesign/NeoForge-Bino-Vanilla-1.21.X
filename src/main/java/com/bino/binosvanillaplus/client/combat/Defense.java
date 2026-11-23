package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatActionPayload;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.CombatMove;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// Tecla DEFENDER (R, regras de 23/11): apertar na hora certa do golpe = PARRY (cores branco/amarelo/azul; com escudo
// no maximo amarelo, sem Issen); apertar com flecha chegando = aparar a flecha; SEGURAR = bloquear (no maximo 2 s).
// Bloqueio: 100% do dano, mas gasta 40 de estamina (20 com escudo) e enche a postura com 1,5x o golpe (servidor).
// Segurar sem levar golpe gasta 20. Estamina zerada defendendo = guarda quebrada (atordoado 1,2 s, leva 1,5x de dano).
// A postura do jogador aparece numa barrinha acima da barra de atalhos so quando tem algo nela.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Defense {
    private Defense() {}

    public static final KeyMapping DEFEND = new KeyMapping("key.binosvanillaplus.defend", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.binosvanillaplus");
    public static final long MAX_HOLD_MS = 2000;

    private static boolean holding, gotHit;
    private static long holdStart;
    private static float posture;
    private static long brokenUntil, postureAt;

    private static long now() { return System.currentTimeMillis(); }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) { e.register(DEFEND); }

    public static boolean blocking() { return holding; }
    public static boolean stunned() { return now() < brokenUntil; }

    // o servidor confirmou um bloqueio: gasta a estamina; zerou = guarda quebrada
    static void blocked(float cost) {
        gotHit = true;
        if (Stamina.drain(cost)) {
            PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.GUARD_BROKEN, 0, (byte) 0));
            stop(false);
        }
        HitFx.kick(0.5f);
    }

    static void posture(float frac, boolean broken) {
        posture = frac;
        postureAt = now();
        if (broken) { brokenUntil = now() + 1200; stop(false); }
    }

    private static void stop(boolean send) {
        if (!holding) return;
        holding = false;
        PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.BLOCK_END, 0, (byte) 0));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { holding = false; posture = 0; brokenUntil = 0; return; }
        long now = now();
        boolean weapon = CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE;
        boolean shield = p.getOffhandItem().getItem() instanceof ShieldItem;
        while (DEFEND.consumeClick()) {
            if (mc.screen != null || (!weapon && !shield) || stunned() || DodgeRoll.rolling(p) || holding) continue;
            // 1) golpe com aviso vindo agora: parry
            int mob = CombatFx.parryTarget(p, now);
            if (mob != -1) {
                byte zone = (byte) Math.max(0, CombatFx.zone(mob, now));
                if (!weapon) zone = (byte) Math.min(zone, 1);   // escudo: sem Issen
                CombatFx.consumeIncoming(mob);
                if (weapon) CombatMoves.special(p, CombatMove.PARRY, CombatActionPayload.PARRY, mob, zone, now);
                else PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.PARRY, mob, zone));
                continue;
            }
            // 2) flecha/projetil chegando: apara (mirando nele, volta para quem atirou)
            int[] arrow = weapon ? CombatMoves.arrowIncoming(p) : null;
            if (arrow != null) {
                CombatMoves.special(p, CombatMove.PARRY, CombatActionPayload.ARROW_PARRY, arrow[0], (byte) arrow[1], now);
                continue;
            }
            // 3) bloquear enquanto segurar (no maximo 2 s)
            if (Stamina.combat() <= 0) { Stamina.denied(); continue; }
            holding = true; gotHit = false; holdStart = now;
            PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.BLOCK_START, 0, (byte) (shield && !weapon ? 1 : 0)));
        }
        if (holding && (!DEFEND.isDown() || now - holdStart >= MAX_HOLD_MS || mc.screen != null || (!weapon && !shield))) {
            if (!gotHit) Stamina.drain(Stamina.BLOCK_WASTED);   // segurou a toa
            stop(true);
        }
    }

    // barrinha da postura do jogador (aparece so quando tem algo nela)
    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        long now = now();
        boolean broken = stunned();
        float f = broken ? 1f : posture;
        if (f < 0.03f && !broken) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = 82, x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() - 22 - 30;
        g.fill(x - 1, y - 1, x + w + 1, y + 4, 0xA0101010);
        int fill = Math.round(w * Mth.clamp(f, 0f, 1f));
        int col = broken ? ((now / 90) % 2 == 0 ? 0xFFFF3030 : 0xFFFFD040) : f < 0.5f ? 0xFFF2C14E : f < 0.8f ? 0xFFF08A30 : 0xFFE84030;
        // enche do meio para as pontas (como em Sekiro)
        int half = fill / 2;
        g.fill(x + w / 2 - half, y, x + w / 2 + half, y + 3, col);
        if (holding) g.fill(x + w / 2 - 1, y - 2, x + w / 2 + 1, y + 5, 0xFFFFFFFF);
    }
}
