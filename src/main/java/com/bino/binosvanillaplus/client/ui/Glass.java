package com.bino.binosvanillaplus.client.ui;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

// Vidro dos menus ("vidro liquido"): o mundo atras do menu e' copiado e desfocado uma vez por quadro (capture),
// e cada painel/botao/slot desenha esse fundo com tinta, refracao na borda e fio de luz (panel).
// Sem os shaders (falhou ao carregar) cai num retangulo arredondado comum.
public final class Glass {
    private Glass() {}

    public static ShaderInstance blur, glass;   // registrados em MapClientEvents
    private static TextureTarget a, b;
    private static VertexBuffer quad;
    private static boolean ready;

    // estilo de um pedaco de vidro
    public record Style(float r, float g, float b, float tint, float refract, float band, float rim, float milk, boolean recess) {
        public Style withTint(float t) { return new Style(r, g, b, t, refract, band, rim, milk, recess); }
        public Style withMilk(float m) { return new Style(r, g, b, tint, refract, band, rim, m, recess); }
        public Style withRim(float k) { return new Style(r, g, b, tint, refract, band, k, milk, recess); }
    }

    // vidro escuro dos paineis, poco escuro dos slots e vidro claro dos botoes
    public static final Style PANEL = new Style(0.07f, 0.075f, 0.08f, 0.55f, 13f, 17f, 1.0f, 0.0f, false);
    public static final Style SLOT = new Style(0.03f, 0.03f, 0.035f, 0.55f, 4f, 6f, 0.55f, 0.0f, true);
    public static final Style BUTTON = new Style(0.86f, 0.87f, 0.88f, 0.70f, 5f, 7f, 0.9f, 0.02f, false);
    public static final Style POPUP = new Style(0.06f, 0.065f, 0.07f, 0.68f, 8f, 10f, 1.0f, 0.0f, false);

    public static boolean available() { return blur != null && glass != null; }

    // copia o mundo atras do menu, em 1/4 da resolucao, e desfoca (4 passadas). Chamar antes de desenhar o menu.
    public static void capture() {
        ready = false;
        if (!available()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.getMainRenderTarget();
            int w = Math.max(1, main.width / 4), h = Math.max(1, main.height / 4);
            if (a == null) {
                a = new TextureTarget(w, h, false, Minecraft.ON_OSX);
                b = new TextureTarget(w, h, false, Minecraft.ON_OSX);
                a.setFilterMode(GL11.GL_LINEAR);
                b.setFilterMode(GL11.GL_LINEAR);
            } else if (a.width != w || a.height != h) {
                a.resize(w, h, Minecraft.ON_OSX);
                b.resize(w, h, Minecraft.ON_OSX);
                a.setFilterMode(GL11.GL_LINEAR);
                b.setFilterMode(GL11.GL_LINEAR);
            }
            if (quad == null) {
                BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bb.addVertex(-1f, -1f, 0f); bb.addVertex(1f, -1f, 0f); bb.addVertex(1f, 1f, 0f); bb.addVertex(-1f, 1f, 0f);
                MeshData md = bb.build();
                quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
                quad.bind();
                if (md != null) quad.upload(md);
                VertexBuffer.unbind();
            }
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            pass(main.getColorTextureId(), a, 3.0f / main.width, 0f);
            pass(a.getColorTextureId(), b, 0f, 1.6f / h);
            pass(b.getColorTextureId(), a, 1.6f / w, 0f);
            pass(a.getColorTextureId(), b, 0f, 1.6f / h);
            main.bindWrite(true);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ready = true;
        } catch (Throwable t) {
            BinosVanillaMod.LOGGER.error("Vidro dos menus falhou; usando o estilo simples", t);
            blur = null;
            glass = null;
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    private static void pass(int src, TextureTarget dst, float dx, float dy) {
        dst.bindWrite(true);
        blur.setSampler("Src", src);
        blur.safeGetUniform("Dir").set(dx, dy);
        quad.bind();
        quad.drawWithShader(new Matrix4f(), new Matrix4f(), blur);
        VertexBuffer.unbind();
    }

    // sombra suave embaixo de um pedaco de vidro (coordenadas de GUI)
    public static void shadow(GuiGraphics g, float x, float y, float w, float h, float radius, float offset, float spread, float alpha) {
        if (!ready) return;
        draw(g, x, y + offset, w, h, radius, spread, null, alpha, spread);
    }

    // um pedaco de vidro (coordenadas de GUI). 'fallback' = cor usada se o vidro nao estiver disponivel.
    public static void panel(GuiGraphics g, float x, float y, float w, float h, float radius, Style st, int fallback) {
        if (!ready) { RoundRect.fill(g, x, y, w, h, radius, fallback); return; }
        draw(g, x, y, w, h, radius, 0, st, 0, 0);
    }

    private static void draw(GuiGraphics g, float x, float y, float w, float h, float radius, float margin, Style st, float shadow, float spread) {
        g.flush();
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        float sc = (float) mc.getWindow().getGuiScale();
        ShaderInstance sh = glass;
        sh.setSampler("Backdrop", b.getColorTextureId());
        sh.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
        sh.safeGetUniform("Rect").set(x * sc, main.height - (y + h) * sc, w * sc, h * sc);
        sh.safeGetUniform("Radius").set(radius * sc);
        if (st != null) {
            sh.safeGetUniform("Tint").set(st.r(), st.g(), st.b(), st.tint());
            sh.safeGetUniform("Params").set(st.refract() * sc / 2f, st.band() * sc / 2f, st.rim(), st.milk());
            sh.safeGetUniform("Mode").set(st.recess() ? 1f : 0f, sc, 0f, 1f);
        } else {
            sh.safeGetUniform("Mode").set(0f, sc, shadow, spread * sc);
        }
        RenderSystem.setShader(() -> sh);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        // coordenadas ja finais (sem a pose da GUI): o retangulo do shader e' o mesmo do quad
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        float x0 = x - margin * 2, y0 = y - margin * 2, x1 = x + w + margin * 2, y1 = y + h + margin * 2;
        bb.addVertex(x0, y1, 0); bb.addVertex(x1, y1, 0); bb.addVertex(x1, y0, 0); bb.addVertex(x0, y0, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableDepthTest();
    }
}
