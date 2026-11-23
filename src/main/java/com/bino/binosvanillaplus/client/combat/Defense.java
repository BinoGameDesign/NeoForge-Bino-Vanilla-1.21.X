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

// Tecla DEFENDER (R; o "recarregar shaders" do Iris, que tambem era R, passa para a V. Regras de 23/11): apertar na hora certa do golpe = PARRY (cores branco/amarelo/azul; com escudo
// no maximo amarelo, sem Issen); apertar com flecha chegando = aparar a flecha; SEGURAR = bloquear (no maximo 2 s).
// Bloqueio: 100% do dano, mas gasta 40 de estamina (20 com escudo) e enche a postura com 1,5x o golpe (servidor).
// Segurar sem levar golpe gasta 20. Estamina zerada defendendo = guarda quebrada (atordoado 1,2 s, leva 1,5x de dano).
// A postura do jogador nao aparece na tela (so conta por dentro).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Defense {
    private Defense() {}

    public static final KeyMapping DEFEND = new KeyMapping("key.binosvanillaplus.guard", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.binosvanillaplus");   // o "recarregar shaders" do Iris vai para a V (IrisKeyFix)
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

    // (a postura do jogador nao aparece na tela: so conta por dentro)
}
