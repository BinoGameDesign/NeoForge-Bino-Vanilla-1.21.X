package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

// Cabeca 3D do jogador no mapa-mundi, com a skin dele (cabeca + camada de cima), virada para onde ele olha
// (giro e inclinacao): substitui a setinha. Cubo de 1x1x1 centrado na origem, frente para +Z (yaw 0 = sul).
public final class PlayerHead3D {
    private PlayerHead3D() {}

    private static VertexBuffer mesh, hull;

    // casca branca um pouco maior: desenhando so as faces de TRAS dela, sobra uma borda branca so na silhueta
    private static VertexBuffer hull() {
        if (hull != null) return hull;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float h = 0.62f;
        float[][][] faces = {
                {{-h, h, h}, {-h, -h, h}, {h, -h, h}, {h, h, h}},
                {{h, h, -h}, {h, -h, -h}, {-h, -h, -h}, {-h, h, -h}},
                {{-h, h, -h}, {-h, -h, -h}, {-h, -h, h}, {-h, h, h}},
                {{h, h, h}, {h, -h, h}, {h, -h, -h}, {h, h, -h}},
                {{-h, h, -h}, {-h, h, h}, {h, h, h}, {h, h, -h}},
                {{-h, -h, h}, {-h, -h, -h}, {h, -h, -h}, {h, -h, h}}};
        for (float[][] f : faces) for (float[] v : f) b.addVertex(v[0], v[1], v[2]).setColor(255, 255, 255, 255);
        MeshData md = b.build();
        hull = new VertexBuffer(VertexBuffer.Usage.STATIC);
        hull.bind();
        if (md != null) hull.upload(md);
        VertexBuffer.unbind();
        return hull;
    }

    // faces com o "rosto" da skin (64x64). u0..u1 sempre da esquerda para a direita de quem olha a face de fora.
    private static void cube(BufferBuilder b, float h, int ou) {
        float s = 1f / 64f;
        // frente (+Z): u 8..16, esquerda de quem olha = -X
        face(b, -h, h, h, h, h, h, h, -h, h, -h, -h, h, (ou + 8) * s, 8 * s, (ou + 16) * s, 16 * s, 0.86f);
        // tras (-Z): u 24..32, esquerda de quem olha = +X
        face(b, h, h, -h, -h, h, -h, -h, -h, -h, h, -h, -h, (ou + 24) * s, 8 * s, (ou + 32) * s, 16 * s, 0.86f);
        // lado direito do personagem (-X): u 0..8, esquerda de quem olha = -Z
        face(b, -h, h, -h, -h, h, h, -h, -h, h, -h, -h, -h, (ou) * s, 8 * s, (ou + 8) * s, 16 * s, 0.72f);
        // lado esquerdo do personagem (+X): u 16..24, esquerda de quem olha = +Z
        face(b, h, h, h, h, h, -h, h, -h, -h, h, -h, h, (ou + 16) * s, 8 * s, (ou + 24) * s, 16 * s, 0.72f);
        // topo (+Y): u 8..16, v 0 (tras) .. 8 (frente)
        face(b, -h, h, -h, h, h, -h, h, h, h, -h, h, h, (ou + 8) * s, 0, (ou + 16) * s, 8 * s, 1.0f);
        // baixo (-Y): u 16..24
        face(b, -h, -h, h, h, -h, h, h, -h, -h, -h, -h, -h, (ou + 16) * s, 0, (ou + 24) * s, 8 * s, 0.55f);
    }

    // a=cima-esquerda, b=cima-direita, c=baixo-direita, d=baixo-esquerda
    private static void face(BufferBuilder bb, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float shade) {
        int c = (int) (255 * shade);
        bb.addVertex(ax, ay, az).setUv(u0, v0).setColor(c, c, c, 255);
        bb.addVertex(dx, dy, dz).setUv(u0, v1).setColor(c, c, c, 255);
        bb.addVertex(cx, cy, cz).setUv(u1, v1).setColor(c, c, c, 255);
        bb.addVertex(bx, by, bz).setUv(u1, v0).setColor(c, c, c, 255);
    }

    private static VertexBuffer mesh() {
        if (mesh != null) return mesh;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        cube(b, 0.5f, 0);          // cabeca
        cube(b, 0.5625f, 32);      // camada de cima (chapeu/capuz da skin), um pouco maior
        MeshData md = b.build();
        mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.bind();
        if (md != null) mesh.upload(md);
        VertexBuffer.unbind();
        return mesh;
    }

    // mv ja posicionado no centro da cabeca; size = aresta em blocos
    public static void draw(Matrix4f mv, Matrix4f proj, AbstractClientPlayer p, float size) {
        ShaderInstance sh = GameRenderer.getPositionTexColorShader();
        if (sh == null) return;
        float yaw = (float) Math.toRadians(p.getYHeadRot());
        float pitch = (float) Math.toRadians(Math.max(-60f, Math.min(60f, p.getXRot())));
        Matrix4f m = new Matrix4f(mv).rotateY(-yaw).rotateX(pitch).scale(size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        // 1) contorno: faces de tras da casca branca (as da frente sao descartadas)
        ShaderInstance pc = GameRenderer.getPositionColorShader();
        if (pc != null) {
            RenderSystem.enableCull();
            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_FRONT);
            VertexBuffer hb = hull();
            hb.bind();
            hb.drawWithShader(m, proj, pc);
            VertexBuffer.unbind();
            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_BACK);
        }
        // 2) a cabeca com a skin por cima
        RenderSystem.setShaderTexture(0, p.getSkin().texture());
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        VertexBuffer vb = mesh();
        vb.bind();
        vb.drawWithShader(m, proj, sh);
        VertexBuffer.unbind();
        RenderSystem.enableCull();
    }
}
