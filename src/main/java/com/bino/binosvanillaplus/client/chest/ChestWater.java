package com.bino.binosvanillaplus.client.chest;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

// Animacao de agua do Bino ("water" no Blockbench): bau debaixo d'agua (alagado) que de vez em quando levanta a tampa
// e solta bolhas. Vale para todos os baus, o mimico disfarcado (e' o proprio bau) e o mimico revelado quando parado.
//  - Cada bau sorteia o seu tempo (15 a 40 s entre uma e outra; a primeira entre 5 e 40 s), entao nao sincronizam.
//  - Relogio do jogo (pausa junto com o jogo). So no cliente, nada vai para o servidor.
//  - As bolhas saem nos dois momentos em que a tampa sobe na animacao do Bino.
public final class ChestWater {
    private ChestWater() {}

    public static final int FIRST_MIN_MS = 5_000, FIRST_MAX_MS = 40_000;
    public static final int GAP_MIN_MS = 15_000, GAP_MAX_MS = 40_000;
    /** momentos (s) da animacao em que a tampa abre uma fresta e as bolhas escapam, e quantas bolhas em cada um */
    private static final float[] PUFF_AT = {0.07f, 0.36f};
    private static final int[] PUFF_COUNT = {4, 9};

    // {inicio da animacao em ms de jogo (-1 = parada), proximo inicio, bolhadas ja soltas (bits)}
    private static final Map<Object, long[]> STATE = new WeakHashMap<>();

    /**
     * Avanca o relogio da animacao de agua de um bau (ou mimico).
     * @param active  esta debaixo d'agua e livre para animar (fechado, parado)
     * @return ms dentro da animacao, ou -1 quando nao esta tocando. Solta as bolhas pelo {@code puff}.
     */
    public static long step(Object key, Level level, float partialTick, boolean active, float lengthS, Puff puff) {
        long now = (long) ((level.getGameTime() + partialTick) * 50.0);
        long[] s = STATE.get(key);
        if (s == null) {
            s = new long[]{-1, now + between(level.random, FIRST_MIN_MS, FIRST_MAX_MS), 0};
            STATE.put(key, s);
        }
        if (!active) {
            if (s[0] >= 0) { s[0] = -1; s[1] = now + between(level.random, GAP_MIN_MS, GAP_MAX_MS); }
            return -1;
        }
        if (s[0] < 0) {
            if (now < s[1]) return -1;
            s[0] = now; s[2] = 0;
        }
        long t = now - s[0];
        if (t >= (long) (lengthS * 1000f)) {
            s[0] = -1; s[1] = now + between(level.random, GAP_MIN_MS, GAP_MAX_MS);
            return -1;
        }
        for (int i = 0; i < PUFF_AT.length; i++) {
            if ((s[2] & (1L << i)) == 0 && t >= PUFF_AT[i] * 1000f) {
                s[2] |= 1L << i;
                puff.at(PUFF_COUNT[i], i == PUFF_AT.length - 1);
            }
        }
        return t;
    }

    public interface Puff { void at(int count, boolean big); }

    /**
     * Bolhas saindo pela fresta da tampa: pela frente e pelos lados, na altura da emenda, subindo.
     * @param x,y,z   centro do bau na altura da emenda da tampa
     * @param fx,fz   para onde a frente (fechadura) aponta
     * @param half    meia largura do bau (0.45 simples, ~0.95 duplo)
     */
    public static void bubbles(Level level, double x, double y, double z, double fx, double fz, double half, int count, boolean big) {
        RandomSource r = level.random;
        double sx = -fz, sz = fx;   // de lado
        for (int i = 0; i < count; i++) {
            double u = (r.nextDouble() * 2 - 1) * half;
            double front = 0.38 + r.nextDouble() * 0.1;
            boolean side = r.nextFloat() < 0.2f;          // algumas escapam pelas laterais
            double px = x + (side ? fx * (r.nextDouble() - 0.5) * 0.8 + sx * Math.copySign(half, u) : fx * front + sx * u);
            double pz = z + (side ? fz * (r.nextDouble() - 0.5) * 0.8 + sz * Math.copySign(half, u) : fz * front + sz * u);
            double py = y + (r.nextDouble() - 0.5) * 0.06;
            double out = 0.02 + r.nextDouble() * 0.03;
            level.addParticle(ParticleTypes.BUBBLE, px, py, pz,
                    (side ? sx * Math.signum(u) : fx) * out, 0.05 + r.nextDouble() * 0.08, (side ? sz * Math.signum(u) : fz) * out);
        }
        if (big) {   // algumas maiores sobem pelo meio da tampa
            for (int i = 0; i < 2; i++)
                level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x + sx * (r.nextDouble() - 0.5) * half, y + 0.35, z + sz * (r.nextDouble() - 0.5) * half, 0, 0.04, 0);
        }
        level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS,
                big ? 0.35f : 0.18f, 1.15f + r.nextFloat() * 0.25f, false);
    }

    private static int between(RandomSource r, int a, int b) { return a + r.nextInt(b - a + 1); }
}
