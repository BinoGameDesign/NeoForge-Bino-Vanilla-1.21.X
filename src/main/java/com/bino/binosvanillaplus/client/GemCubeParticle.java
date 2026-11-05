package com.bino.binosvanillaplus.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Gema que cai do minerio: um cubinho 3D (modelo "gem_particule" do Bino) que gira enquanto cai, quica e brilha.
// Textura 64x64 em textures/block/particle/<gema>_gem_particle.png; cada face usa um quadro de 16x16:
//   norte (0,0)  sul (16,0)  baixo (32,0)  leste (0,16)  oeste (16,16)  cima (0,32)
public class GemCubeParticle extends Particle {
    // {direcao x,y,z, canto da textura u,v (em pixels de 64)}
    private static final float[][] FACES = {
            {0, 0, -1, 0, 0}, {0, 0, 1, 16, 0}, {0, -1, 0, 32, 0}, {1, 0, 0, 0, 16}, {-1, 0, 0, 16, 16}, {0, 1, 0, 0, 32}};
    private static final float[] SHADE = {0.8f, 0.8f, 0.5f, 0.6f, 0.6f, 1.0f};   // como os blocos do jogo

    private final TextureAtlasSprite sprite;
    private final float size;
    private final Vector3f axis;
    private final float spinSpeed;
    private float angle, oAngle;

    public GemCubeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.sprite = sprite;
        xd = vx; yd = vy; zd = vz;
        gravity = 1.0f;
        friction = 0.96f;
        hasPhysics = true;
        size = 0.05f + random.nextFloat() * 0.035f;   // meio lado do cubo, em blocos
        setSize(size * 2, size * 2);
        lifetime = 35 + random.nextInt(25);
        axis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
        spinSpeed = 0.25f + random.nextFloat() * 0.35f;
        angle = oAngle = random.nextFloat() * 6.28f;
    }

    @Override
    public void tick() {
        super.tick();
        oAngle = angle;
        if (!onGround) angle += spinSpeed;
        else { xd *= 0.6; zd *= 0.6; }
        if (age > lifetime - 8) alpha = Math.max(0f, (lifetime - age) / 8f);
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        Vec3 cam = camera.getPosition();
        float cx = (float) (Mth.lerp(pt, xo, x) - cam.x()), cy = (float) (Mth.lerp(pt, yo, y) - cam.y()) + size, cz = (float) (Mth.lerp(pt, zo, z) - cam.z());
        Quaternionf rot = new Quaternionf().fromAxisAngleRad(axis, Mth.lerp(pt, oAngle, angle));
        int light = 0xF000F0;   // a gema brilha
        for (int f = 0; f < 6; f++) {
            Vector3f n = new Vector3f(FACES[f][0], FACES[f][1], FACES[f][2]);
            Vector3f v = Math.abs(n.y) > 0.5f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);
            Vector3f u = new Vector3f(v).cross(n);   // u x v = n -> vertices no sentido anti-horario vistos de fora
            float u0 = sprite.getU(FACES[f][3] / 64f), u1 = sprite.getU((FACES[f][3] + 16) / 64f);
            float v0 = sprite.getV(FACES[f][4] / 64f), v1 = sprite.getV((FACES[f][4] + 16) / 64f);
            float s = SHADE[f];
            corner(vc, rot, n, u, v, -1, -1, cx, cy, cz, u0, v1, s, light);
            corner(vc, rot, n, u, v, 1, -1, cx, cy, cz, u1, v1, s, light);
            corner(vc, rot, n, u, v, 1, 1, cx, cy, cz, u1, v0, s, light);
            corner(vc, rot, n, u, v, -1, 1, cx, cy, cz, u0, v0, s, light);
        }
    }

    private void corner(VertexConsumer vc, Quaternionf rot, Vector3f n, Vector3f u, Vector3f v, int su, int sv,
                        float cx, float cy, float cz, float tu, float tv, float shade, int light) {
        Vector3f p = new Vector3f(n).add(u.x * su, u.y * su, u.z * su).add(v.x * sv, v.y * sv, v.z * sv).mul(size);
        rot.transform(p);
        vc.addVertex(cx + p.x, cy + p.y, cz + p.z).setUv(tu, tv).setColor(shade, shade, shade, alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
}
