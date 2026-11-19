package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.HeldToolModel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// Contorno da forja (+4, +6, +8, +10) no item 3D: na mao (1a e 3a pessoa), no chao e em moldura. Codigo proprio.
// E' uma borda de verdade em 3D, presa ao item (gira e se move com ele), sem efeito de tela e sem brilho espalhado:
//  - Item 2D (pixel art extrudado, como a ferramenta de madeira): um anel de 1 pixel em volta do desenho, com a
//    mesma espessura do item. As cores sao as do contorno 2D do inventario (ForgeIconOutline: o degrade do Bino,
//    branco nos pontos de brilho indo para a cor principal).
//  - Modelo 3D (ferramentas de gema): "casca invertida": as faces do modelo empurradas para fora e viradas do avesso;
//    so o lado de tras aparece, entao sobra uma borda em volta da silhueta. Degrade branco (cima-direita) -> cor.
// O nivel muda a opacidade (e, no modelo 3D, a espessura).
public final class ForgeOutline {
    private ForgeOutline() {}

    // ====== AJUSTES ======
    private static final float[] ALPHA = {0f, 0.55f, 0.72f, 0.88f, 1.00f};       // +4, +6, +8, +10
    private static final float[] SHELL_PX = {0f, 0.55f, 0.65f, 0.75f, 0.85f};    // espessura da casca 3D (pixels do modelo)

    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps,
                              MultiBufferSource buf, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer()) return;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentCull(white()));
        boolean solid3d = model instanceof HeldToolModel h && shown == h.solid();
        if (solid3d) shell(vc, ps.last(), shown, stack, tier);
        else ring(vc, ps.last(), stack, tier);
        ps.popPose();
    }

    // ---------------------------------------------------------------- item 2D: anel de pixels
    private static void ring(VertexConsumer vc, PoseStack.Pose pose, ItemStack stack, int tier) {
        NativeImage img = ForgeIconOutline.image(stack.getItem());
        if (img == null) return;
        int W = img.getWidth(), H = img.getHeight();
        float s = 16f / (W - 2) / 16f;          // tamanho de 1 pixel do sprite, em blocos
        float z0 = 7.5f / 16f, z1 = 8.5f / 16f;  // mesma espessura do item extrudado
        float a = ALPHA[tier];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int c = img.getPixelRGBA(x, y);   // ABGR
                if ((c >>> 24) < 16) continue;
                float r = (c & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = ((c >> 16) & 255) / 255f;
                float x0 = (x - 1) * s, x1 = x0 + s;
                float y1 = 1f - (y - 1) * s, y0 = y1 - s;   // linha da imagem cresce para baixo
                box(vc, pose, x0, y0, z0, x1, y1, z1, r, g, b, a,
                        !on(img, x, y - 1), !on(img, x, y + 1), !on(img, x - 1, y), !on(img, x + 1, y));
            }
    }

    private static boolean on(NativeImage img, int x, int y) {
        return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && (img.getPixelRGBA(x, y) >>> 24) >= 16;
    }

    // caixa de 1 pixel; as faces laterais so onde nao ha vizinho (evita faces internas na transparencia)
    private static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
                            float r, float g, float b, float a, boolean top, boolean bottom, boolean left, boolean right) {
        quad(vc, p, r, g, b, a, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);   // frente
        quad(vc, p, r, g, b, a, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);  // tras
        if (top) quad(vc, p, r, g, b, a, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        if (bottom) quad(vc, p, r, g, b, a, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        if (left) quad(vc, p, r, g, b, a, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        if (right) quad(vc, p, r, g, b, a, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float r, float g, float b, float a, float nx, float ny, float nz,
                             float... v) {
        for (int i = 0; i < 4; i++)
            vc.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(r, g, b, a).setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, nx, ny, nz);
    }

    // ---------------------------------------------------------------- modelo 3D: casca invertida
    // por modelo: as posicoes dos vertices (em blocos) e a direcao para "inflar" cada um (soma das normais das faces
    // que encostam naquele ponto: num cubo, o canto anda na diagonal e a caixa cresce igual em todos os lados)
    private record Shell(float[] pos, float[] dir, float[] nrm, float minY, float maxY, float minX, float maxX) {}
    private static final Map<BakedModel, Shell> SHELLS = new IdentityHashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static void shell(VertexConsumer vc, PoseStack.Pose p, BakedModel model, ItemStack stack, int tier) {
        Shell sh = SHELLS.computeIfAbsent(model, m -> buildShell(m, stack));
        if (sh.pos.length == 0) return;
        float d = SHELL_PX[tier] / 16f, a = ALPHA[tier];
        int rgb = ForgeGlow.color(stack.getItem());
        float br = ((rgb >> 16) & 255) / 255f, bg = ((rgb >> 8) & 255) / 255f, bb = (rgb & 255) / 255f;
        float span = Math.max(1e-3f, (sh.maxX - sh.minX) + (sh.maxY - sh.minY));
        for (int q = 0; q < sh.pos.length / 12; q++) {
            for (int k = 3; k >= 0; k--) {   // ordem invertida: a face fica visivel so por dentro
                int i = q * 12 + k * 3;
                float x = sh.pos[i] + sh.dir[i] * d, y = sh.pos[i + 1] + sh.dir[i + 1] * d, z = sh.pos[i + 2] + sh.dir[i + 2] * d;
                // degrade no proprio modelo: ponta de cima (cabeca) mais branca, cabo na cor do item
                float t = ((x - sh.minX) + (y - sh.minY)) / span;
                float w = 0.15f + 0.85f * t * t;
                vc.addVertex(p, x, y, z).setColor(br + (1 - br) * w, bg + (1 - bg) * w, bb + (1 - bb) * w, a).setUv(0.5f, 0.5f)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(p, -sh.nrm[q * 3], -sh.nrm[q * 3 + 1], -sh.nrm[q * 3 + 2]);
            }
        }
    }

    private static Shell buildShell(BakedModel model, ItemStack stack) {
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (Direction dir : Direction.values()) { RANDOM.setSeed(42L); quads.addAll(pass.getQuads(null, dir, RANDOM)); }
            RANDOM.setSeed(42L);
            quads.addAll(pass.getQuads(null, null, RANDOM));
        }
        int n = quads.size();
        float[] pos = new float[n * 12], nrm = new float[n * 3];
        Map<Long, List<float[]>> normalsAt = new HashMap<>();
        float minX = 1e9f, maxX = -1e9f, minY = 1e9f, maxY = -1e9f;
        for (int q = 0; q < n; q++) {
            int[] v = quads.get(q).getVertices();
            int stride = v.length / 4;
            for (int k = 0; k < 4; k++)
                for (int c = 0; c < 3; c++) pos[q * 12 + k * 3 + c] = Float.intBitsToFloat(v[k * stride + c]);
            Vector3f e1 = new Vector3f(pos[q * 12 + 3] - pos[q * 12], pos[q * 12 + 4] - pos[q * 12 + 1], pos[q * 12 + 5] - pos[q * 12 + 2]);
            Vector3f e2 = new Vector3f(pos[q * 12 + 6] - pos[q * 12], pos[q * 12 + 7] - pos[q * 12 + 1], pos[q * 12 + 8] - pos[q * 12 + 2]);
            Vector3f nv = e1.cross(e2);
            if (nv.lengthSquared() < 1e-12f) { Direction f = quads.get(q).getDirection(); nv.set(f.getStepX(), f.getStepY(), f.getStepZ()); }
            nv.normalize();
            nrm[q * 3] = nv.x; nrm[q * 3 + 1] = nv.y; nrm[q * 3 + 2] = nv.z;
            for (int k = 0; k < 4; k++) {
                float x = pos[q * 12 + k * 3], y = pos[q * 12 + k * 3 + 1];
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                List<float[]> list = normalsAt.computeIfAbsent(key(pos, q * 12 + k * 3), kk -> new ArrayList<>());
                boolean dup = false;
                for (float[] o : list) if (Math.abs(o[0] - nv.x) + Math.abs(o[1] - nv.y) + Math.abs(o[2] - nv.z) < 0.02f) { dup = true; break; }
                if (!dup) list.add(new float[]{nv.x, nv.y, nv.z});
            }
        }
        float[] dir = new float[n * 12];
        for (int i = 0; i < n * 4; i++) {
            float sx = 0, sy = 0, sz = 0;
            for (float[] o : normalsAt.get(key(pos, i * 3))) { sx += o[0]; sy += o[1]; sz += o[2]; }
            // limita para nao estourar em pontos onde muitas faces se encontram
            float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (len > 1.8f) { sx *= 1.8f / len; sy *= 1.8f / len; sz *= 1.8f / len; }
            dir[i * 3] = sx; dir[i * 3 + 1] = sy; dir[i * 3 + 2] = sz;
        }
        return new Shell(pos, dir, nrm, minY, maxY, minX, maxX);
    }

    private static long key(float[] pos, int i) {
        long x = Math.round(pos[i] * 1024), y = Math.round(pos[i + 1] * 1024), z = Math.round(pos[i + 2] * 1024);
        return (x & 0x1FFFFF) << 42 | (y & 0x1FFFFF) << 21 | (z & 0x1FFFFF);
    }

    // ---------------------------------------------------------------- textura branca
    private static ResourceLocation white;

    private static ResourceLocation white() {
        if (white == null) {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixelRGBA(0, 0, 0xFFFFFFFF);
            white = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "forge_outline_white");
            Minecraft.getInstance().getTextureManager().register(white, new DynamicTexture(img));
        }
        return white;
    }

    public static void clearCache() { SHELLS.clear(); }
}
