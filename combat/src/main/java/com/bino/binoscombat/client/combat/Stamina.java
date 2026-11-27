package com.bino.binoscombat.client.combat;

import com.bino.binoscombat.BinosCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

// Estamina dupla (regras de 23/11, doc "Combate - Refinamento"), num icone so a direita da barra de atalhos:
//  - ANEL de fora = estamina de COMBATE (120 + 2 por ponto de Agilidade): golpe leve 20, pesado 40, avanco 20,
//    rolamento 20, bloqueio errado 40 (escudo 20), segurar a defesa a toa 20. Volta 0,6 s depois do ultimo gasto:
//    40/s, ou 15/s defendendo. Parry +40, levantar o inimigo +40, Issen enche tudo. Sai golpe com pelo menos 1 ponto.
//  - CIRCULO de dentro (pizza) = estamina NORMAL (100): correr 6/s, subir escada/trepadeira 12/s, nado rapido 10/s.
//    Volta 25/s depois de 1 s sem gastar. Zerada: nao corre (ate voltar a 20).
// Desenho: o pixel art do Bino (16x16, desenhado a 2x). O pedaco que some passa por verde-escuro, vermelho e
// amarelo e apaga; o que volta passa por azuis ate o verde (em sentido anti-horario, comecando no topo).
@EventBusSubscriber(modid = BinosCombat.MOD_ID, value = Dist.CLIENT)
public final class Stamina {
    private Stamina() {}

    public static final float LIGHT = 20, HEAVY = 40, DASH = 20, ROLL = 20, BLOCK = 40, BLOCK_SHIELD = 20, BLOCK_WASTED = 20;
    public static final float BASE_COMBAT = 120, NORMAL_MAX = 100;

    private static float combat = BASE_COMBAT, normal = NORMAL_MAX;
    private static long lastUse = -100000, lastNormalUse = -100000, deniedAt = -100000;
    private static boolean winded;   // estamina normal zerou: sem correr ate voltar a 20

    private static long now() { return System.currentTimeMillis(); }

    private static boolean free() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null || p.isCreative() || p.isSpectator();
    }

    public static float combatMax() { return BASE_COMBAT + 2f * com.bino.binoscombat.CombatHooks.clientAgility(); }
    public static float combat() { return free() ? combatMax() : combat; }

    // gasta estamina de combate; com pelo menos 1 ponto a acao sai (e a estamina fica em zero)
    public static boolean use(float cost) {
        if (free()) return true;
        if (combat <= 0) { denied(); return false; }
        combat = Math.max(0, combat - cost);
        lastUse = now();
        return true;
    }

    // gasto sem condicao (bloqueio): true se zerou
    public static boolean drain(float cost) {
        if (free()) return false;
        combat = Math.max(0, combat - cost);
        lastUse = now();
        return combat <= 0;
    }

    public static void give(float v) { combat = Math.min(combatMax(), combat + v); }
    public static void fill() { combat = combatMax(); }
    public static void denied() { deniedAt = now(); }

    public static boolean canSprint() { return free() || !winded; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) { combat = BASE_COMBAT; normal = NORMAL_MAX; return; }
        long now = now();
        float max = combatMax();
        if (combat > max) combat = max;
        if (now - lastUse > 600 && combat < max) combat = Math.min(max, combat + (Defense.blocking() ? 15f : 40f) / 20f);
        if (free()) { normal = NORMAL_MAX; winded = false; return; }
        // estamina normal
        float drain = 0;
        boolean moving = p.getDeltaMovement().horizontalDistanceSqr() > 1e-4;
        if (p.isSprinting() && moving && !p.isSwimming()) drain += 6f;
        if (p.isSwimming() && moving) drain += 10f;
        if (p.onClimbable() && p.getDeltaMovement().y > 0.05) drain += 12f;
        if (drain > 0) { normal = Math.max(0, normal - drain / 20f); lastNormalUse = now; }
        else if (now - lastNormalUse > 1000) normal = Math.min(NORMAL_MAX, normal + 25f / 20f);
        if (normal <= 0) winded = true;
        else if (winded && normal >= 20) winded = false;
        if (winded && p.isSprinting()) p.setSprinting(false);
    }

    // ------------------------------------------------------------------ desenho
    private static final String CLS = "0000011111100000000111111111100000111222222111000112222222222110011222111122211011222133331222111122133333312211112213333331221111221333333122111122133333312211112221333312221101122211112221100112222222222110001112222221110000011111111110000000011111100000";
    private static final int[] COL = {
            0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,
            0x00000000,0x00000000,0x00000000,0xFF222324,0xFF222324,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFF222324,0xFF222324,0x00000000,0x00000000,0x00000000,
            0x00000000,0x00000000,0xFF222324,0xFFFFFFFF,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFFFFFFFF,0xFFFFFFFF,0xFF222324,0x00000000,0x00000000,
            0x00000000,0xFF222324,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF222324,0x00000000,
            0x00000000,0xFF222324,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFF29DB4A,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF222324,0x00000000,
            0xFF222324,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFF29DB4A,0xFFFFFFFF,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF222324,
            0xFF222324,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFFFFFFFF,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE5E,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF222324,
            0xFF222324,0xFFFFFFFF,0xFF29DB4A,0xFF29DB4A,0xFFFFFFFF,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE5E,0xFF40AE5E,0xFFFFFFFF,0xFF47D179,0xFF47D179,0xFFFFFFFF,0xFF222324,
            0xFF222324,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF40AE54,0xFF40AE54,0xFF40AE54,0xFF40AE5E,0xFF40AE5E,0xFF40AE5E,0xFFFFFFFF,0xFF47D179,0xFF47D179,0xFFFFFFFF,0xFF222324,
            0xFF222324,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF40AE54,0xFF40AE54,0xFF40AE5E,0xFF40AE5E,0xFF40AE5E,0xFF40AE5E,0xFFFFFFFF,0xFF47D179,0xFF47D179,0xFFFFFFFF,0xFF222324,
            0xFF222324,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFF47D161,0xFFFFFFFF,0xFF40AE5E,0xFF40AE5E,0xFF40AE5E,0xFF40AE5E,0xFFFFFFFF,0xFF47D179,0xFF47D185,0xFF47D179,0xFFFFFFFF,0xFF222324,
            0x00000000,0xFF222324,0xFFFFFFFF,0xFF47D161,0xFF47D161,0xFF47D179,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFF47D179,0xFF47D179,0xFF47D185,0xFFFFFFFF,0xFF222324,0x00000000,
            0x00000000,0xFF222324,0xFFFFFFFF,0xFF47D161,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D185,0xFF47D185,0xFF47D185,0xFFFFFFFF,0xFF222324,0x00000000,
            0x00000000,0x00000000,0xFF222324,0xFFFFFFFF,0xFFFFFFFF,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D179,0xFF47D185,0xFF47D185,0xFFFFFFFF,0xFFFFFFFF,0xFF222324,0x00000000,0x00000000,
            0x00000000,0x00000000,0x00000000,0xFF222324,0xFF222324,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFF222324,0xFF222324,0x00000000,0x00000000,0x00000000,
            0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0xFF222324,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000};
    private static final long[] LOST = new long[256], GAINED = new long[256];
    private static final boolean[] ON = new boolean[256];
    private static boolean init;

    // posicao do pixel no giro: 0 no topo, crescendo no sentido anti-horario
    private static float order(int i) {
        float x = (i % 16) + 0.5f - 8f, y = (i / 16) + 0.5f - 8f;
        float a = (float) Math.atan2(-x, -y);   // topo = 0, esquerda = +pi/2
        if (a < 0) a += (float) (Math.PI * 2);
        return a / (float) (Math.PI * 2);
    }
    private static final float[] ORDER = new float[256];
    static { for (int i = 0; i < 256; i++) ORDER[i] = order(i); }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || free() || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        GuiGraphics g = e.getGuiGraphics();
        com.bino.binosmovement.client.HudScale.push(g);   // mesmo tamanho do resto do visor
        // (27/11) por cima de tudo do visor: em tela pequena ela encosta no minimapa, e a moldura dele (que vai para o
        // desenho depois, junto com a maquete 3D) passava por cima do circulo
        g.flush();
        g.pose().translate(0, 0, 400);
        try { draw(g); g.flush(); } finally { com.bino.binosmovement.client.HudScale.pop(g); }
    }

    private static void draw(GuiGraphics g) {
        long now = now();
        int s = 2;
        int x0 = g.guiWidth() / 2 + 91 + 2, y0 = g.guiHeight() - 22 - 16 * s;
        float fc = Mth.clamp(combat / combatMax(), 0f, 1f), fn = Mth.clamp(normal / NORMAL_MAX, 0f, 1f);
        boolean flash = now - deniedAt < 350 && (now / 70) % 2 == 0;
        for (int i = 0; i < 256; i++) {
            char c = CLS.charAt(i);
            if (c == '0') continue;
            int col = COL[i];
            if (c == '2' || c == '3') {
                float f = c == '2' ? fc : fn;
                // o pixel some quando o nivel passa por ele (o primeiro a sumir e' o do topo, girando para a esquerda)
                boolean on = 1f - ORDER[i] <= f + 1e-4f;
                if (!init) ON[i] = on;
                if (on != ON[i]) { if (on) GAINED[i] = now; else LOST[i] = now; ON[i] = on; }
                boolean ring = c == '2';
                if (on) {
                    long t = now - GAINED[i];
                    if (t < 300) col = t < 60 ? (i % 2 == 0 ? 0xFF0088FF : 0xFF009EFF) : t < 120 ? 0xFF00B5FF : t < 190 ? 0xFF00E2FF : 0xFF00FFAB;
                } else {
                    long t = now - LOST[i];
                    if (t >= 300 || (t > 220 && ((i * 7) % 3) == 0)) continue;   // apagou
                    col = t < 80 ? (ring ? 0xFF19822D : 0xFF1F4F27) : t < 160 ? (i % 2 == 0 ? 0xFFFF0051 : 0xFFFF003B) : (i % 2 == 0 ? 0xFFFF7F00 : 0xFFFFA100);
                }
                if (ring && flash && !on) col = 0xFFFF2040;
            }
            int px = x0 + (i % 16) * s, py = y0 + (i / 16) * s;
            g.fill(px, py, px + s, py + s, col);
        }
        init = true;
    }
}
