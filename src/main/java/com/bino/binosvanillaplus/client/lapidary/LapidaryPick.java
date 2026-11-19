package com.bino.binosvanillaplus.client.lapidary;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

// Mouse -> raio no mundo. As matrizes da camera sao guardadas a cada quadro (RenderLevelStageEvent, AFTER_SKY) e
// invertidas: o ponto do mouse na tela vira um raio que sai da camera. Mesma ideia do Picking do Cinematic
// Interactions (MIT).
public final class LapidaryPick {
    private LapidaryPick() {}

    private static final Matrix4f VIEW_PROJ = new Matrix4f(), INVERSE = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;
    private static boolean valid;

    public static void capture(RenderLevelStageEvent event) {
        VIEW_PROJ.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        VIEW_PROJ.invert(INVERSE);
        camera = event.getCamera().getPosition();
        valid = true;
    }

    public static Vec3 camera() { return camera; }

    // mouse em -1..1 (x para a direita, y para cima)
    public static double[] mouseNdc() {
        Minecraft mc = Minecraft.getInstance();
        double w = Math.max(1, mc.getWindow().getScreenWidth()), h = Math.max(1, mc.getWindow().getScreenHeight());
        return new double[]{mc.mouseHandler.xpos() / w * 2 - 1, 1 - mc.mouseHandler.ypos() / h * 2};
    }

    // {origem, direcao normalizada}
    @Nullable
    public static Vec3[] ray() {
        if (!valid) return null;
        double[] ndc = mouseNdc();
        Vector4f near = INVERSE.transform(new Vector4f((float) ndc[0], (float) ndc[1], -1f, 1f));
        Vector4f far = INVERSE.transform(new Vector4f((float) ndc[0], (float) ndc[1], 1f, 1f));
        near.div(near.w);
        far.div(far.w);
        Vec3 origin = camera.add(near.x, near.y, near.z);
        Vec3 dir = new Vec3(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
        return new Vec3[]{origin, dir};
    }

    // ponto do mundo -> pixel da tela da GUI {x, y} (ou null se estiver atras da camera)
    @Nullable
    public static double[] project(Vec3 world) {
        if (!valid) return null;
        Vector4f v = VIEW_PROJ.transform(new Vector4f((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z), 1f));
        if (v.w <= 1e-4) return null;
        Minecraft mc = Minecraft.getInstance();
        double gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
        return new double[]{(v.x / v.w * 0.5 + 0.5) * gw, (0.5 - v.y / v.w * 0.5) * gh};
    }

    public static void invalidate() { valid = false; }
}
