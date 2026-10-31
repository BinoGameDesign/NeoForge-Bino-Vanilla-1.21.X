package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

// Ceu e neblina dos mapas. Por padrao copia o jogo (cor do ceu e cor da neblina atuais do Minecraft).
// O futuro mod de ambientacao so precisa trocar o 'provider' para a neblina/ceu do mapa acompanharem os dele:
//     MapAtmosphere.provider = new MeuProvider();
public final class MapAtmosphere {
    private MapAtmosphere() {}

    public interface Provider {
        // cor da neblina / horizonte (r, g, b de 0 a 1)
        float[] fogColor();
        // cor do ceu no alto (r, g, b de 0 a 1)
        float[] skyColor();
        // 1 = normal; maior = neblina mais densa (chega mais perto); menor = mais limpa
        default float fogDensity() { return 1f; }
        // altura das nuvens; NaN = sem nuvens
        default float cloudHeight() {
            Minecraft mc = Minecraft.getInstance();
            return mc.level == null ? Float.NaN : mc.level.effects().getCloudHeight();
        }
    }

    // padrao: igual ao jogo
    public static final Provider VANILLA = new Provider() {
        @Override
        public float[] fogColor() {
            float[] f = RenderSystem.getShaderFogColor();
            return new float[]{f[0], f[1], f[2]};
        }

        @Override
        public float[] skyColor() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return new float[]{0.47f, 0.65f, 1f};
            Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
            Vec3 c = mc.level.getSkyColor(cam, mc.getTimer().getGameTimeDeltaPartialTick(false));
            return new float[]{(float) c.x, (float) c.y, (float) c.z};
        }
    };

    public static Provider provider = VANILLA;
}
