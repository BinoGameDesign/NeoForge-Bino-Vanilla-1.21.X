package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// Sensacao da camera (mod Combat), com ideias de jogos de acao:
//  - LEVOU GOLPE: tremida curta por "trauma" (como em Sekiro, God of War, Hades): cada golpe soma trauma, a tremida e'
//    trauma ao quadrado (golpe fraco quase nao treme, golpe forte treme bem) e some em ~0,5 s. Junto, um tranco
//    pequeno para cima e para o lado de onde veio o golpe. Vale em primeira pessoa, no ombro e em terceira.
//  - MOVIMENTO RAPIDO (rolamento, avanco W W, golpes que levam o corpo): desfoque nos cantos da tela, como os
//    riscos de velocidade de Spider-Man / Devil May Cry, mais um leve aumento do campo de visao. Com shader
//    (Iris) um desfoque de verdade nao roda junto, entao e' uma camada por cima da tela (funciona com e sem shader).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class CameraFeel {
    private CameraFeel() {}

    private static final ResourceLocation BLUR = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/combat/speed_blur.png");

    // ---------------------------------------------------------------- golpe recebido
    private static float trauma, kick, kickSide;
    private static long lastMs;
    private static int lastHurt;
    private static float lastHealth = -1;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { lastHealth = -1; speedTarget = 0; return; }
        // comecou a animacao de dano (o servidor mandou): tremida base + tranco
        if (p.hurtTime > lastHurt && p.hurtTime >= p.hurtDuration - 1) {
            trauma = Math.min(1f, trauma + 0.42f);
            kick = 1f;
            kickSide = -Mth.sin(p.getHurtDir() * Mth.DEG_TO_RAD);
        }
        lastHurt = p.hurtTime;
        // quanto mais vida perdeu, mais treme
        float h = p.getHealth();
        if (lastHealth >= 0 && h < lastHealth) trauma = Math.min(1f, trauma + Math.min(0.55f, (lastHealth - h) / 9f));
        lastHealth = h;

        // velocidade do movimento de combate (rolamento/avanco/golpe que leva o corpo)
        boolean active = DodgeRoll.rolling(p) || CombatMoves.moving(p);
        double hs = p.getDeltaMovement().horizontalDistance();
        speedTarget = active ? Mth.clamp((float) (hs - 0.24) / 0.32f, 0f, 1f) : 0f;
    }

    private static float noise(float t, float seed) {   // "ruido" suave: senos com frequencias que nao se repetem juntas
        return 0.6f * Mth.sin(t * 31f + seed) + 0.4f * Mth.sin(t * 57.3f + seed * 2.3f);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 0f : Mth.clamp((now - lastMs) / 1000f, 0f, 0.1f);
        lastMs = now;
        trauma = Math.max(0f, trauma - dt * 1.9f);
        kick = Math.max(0f, kick - dt * 7f);
        speed += (speedTarget - speed) * Math.min(1f, dt * (speedTarget > speed ? 18f : 6f));
        if (trauma <= 0f && kick <= 0f) return;
        float s = trauma * trauma, t = now / 1000f;
        float k = kick * kick;
        e.setYaw(e.getYaw() + 3.2f * s * noise(t, 1.7f) + 1.4f * k * kickSide);
        e.setPitch(e.getPitch() + 2.6f * s * noise(t, 4.1f) - 2.0f * k);
        e.setRoll(e.getRoll() + 4.0f * s * noise(t, 7.9f) + 2.0f * k * kickSide);
    }

    // ---------------------------------------------------------------- desfoque de velocidade
    private static float speed, speedTarget;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov() || speed < 0.01f) return;
        e.setFOV(e.getFOV() * (1 + 0.07f * speed));
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent e) {
        e.registerBelowAll(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "speed_blur"), CameraFeel::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (speed < 0.02f || Minecraft.getInstance().options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();
        // um pouco maior que a tela e "pulando" de leve: os riscos parecem passar
        float jit = 1.04f + 0.04f * ((Util.getMillis() / 33) % 3) / 2f;
        int bw = (int) (w * 1.12f * jit), bh = (int) (h * 1.12f * jit);   // esticado na tela: os cantos todos pegam
        int x = (w - bw) / 2, y = (h - bh) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 0.6f * speed);
        g.blit(BLUR, x, y, 0, 0, bw, bh, bw, bh);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
