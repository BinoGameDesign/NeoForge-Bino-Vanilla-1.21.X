package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatMove;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

// Animacoes feitas no BLOCKBENCH (terceira pessoa). O Bino anima no Blockbench, o script
// tools/blockbench/importar_animacoes.py passa o .bbmodel para assets/binosvanillaplus/combat_anims/<arma>.json e
// aqui o jogo toca: mesmas curvas do Blockbench (linear, catmullrom, bezier, step) e mesmos ossos do modelo:
//   root (corpo inteiro: pulo/agacha/vai a frente, inclina, rodopio), tumble (cambalhota/tombo, gira no meio do corpo),
//   head, body, right_arm, left_arm, right_leg, left_leg, item (a arma: gira e estica a partir da mao).
// Golpe sem animacao do Blockbench continua com a animacao antiga (MoveAnims).
//
// Eixos: o Blockbench usa Y para cima e o boneco olhando para -Z; o modelo do jogo usa Y para baixo (girado 180 graus
// em Z). Por isso: rotacao do jogo = (-x, -y, z) do Blockbench e deslocamento do jogo = (-x, -y, z).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class BbAnims {
    private BbAnims() {}

    // nome da animacao no Blockbench para cada golpe (as que ainda nao existirem no JSON ficam com a antiga)
    private static String name(CombatMove m) {
        return switch (m) {
            case LIGHT_1 -> "base_1"; case LIGHT_2 -> "base_2"; case LIGHT_3 -> "base_3"; case LIGHT_4 -> "base_4"; case LIGHT_5 -> "base_5";
            case SURPRISE -> "surpresa"; case GUARD_BREAK -> "quebra_guarda"; case JUDGMENT -> "juizo"; case TORMENTA -> "tormenta";
            case LAUNCHER -> "lanca_ceu"; case AIR_1 -> "aereo_1"; case AIR_2 -> "aereo_2"; case AIR_3 -> "aereo_3"; case METEOR -> "meteoro";
            case FORWARD -> "investida"; case DASH -> "avanco"; case DASH_SLASH -> "avanco_corte"; case PIERCE -> "perfurador";
            case ROLL_SLASH -> "rolamento_corte"; case ROLL_THRUST -> "rolamento_estocada"; case PARRY -> "aparar"; case ISSEN -> "issen";
            case RUSH -> "rajada"; case CRITICAL -> "critico"; case BACKSTAB -> "pelas_costas";
            default -> null;
        };
    }

    public static Anim forMove(CombatMove m, boolean axe) {
        String n = name(m);
        return n == null ? null : load(axe ? "axe" : "sword").get(n);
    }

    // ------------------------------------------------------------------ dados

    static final int LIN = 0, CATMULL = 1, BEZIER = 2, STEP = 3;

    record Key(float t, float[] v, int interp, float[] lt, float[] lv, float[] rt, float[] rv) {}

    static final class Channel {
        final Key[] k;
        Channel(Key[] k) { this.k = k; }

        float[] at(float t) {
            float[] o = new float[3];
            for (int a = 0; a < 3; a++) o[a] = axis(t, a);
            return o;
        }

        private float axis(float t, int a) {
            Key[] k = this.k;
            if (t <= k[0].t + 1e-5f) return k[0].v[a];
            if (t >= k[k.length - 1].t - 1e-5f) return k[k.length - 1].v[a];
            for (int i = 0; i < k.length - 1; i++) {
                Key b = k[i], f = k[i + 1];
                if (t > f.t) continue;
                if (Math.abs(f.t - t) < 1e-5f) return f.v[a];
                if (b.interp == STEP) return b.v[a];
                float al = (t - b.t) / Math.max(1e-5f, f.t - b.t);
                if (b.interp == LIN && (f.interp == LIN || f.interp == STEP)) return b.v[a] + (f.v[a] - b.v[a]) * al;
                if (b.interp == CATMULL || f.interp == CATMULL) return catmull(i, a, al);
                if (b.interp == BEZIER || f.interp == BEZIER) return bezier(b, f, a, t);
                return b.v[a] + (f.v[a] - b.v[a]) * al;
            }
            return k[k.length - 1].v[a];
        }

        // igual ao Blockbench (THREE.SplineCurve com os pontos (tempo, valor) vizinhos)
        private float catmull(int i, int a, float al) {
            Key bp = i > 0 ? k[i - 1] : null, ap = i + 2 < k.length ? k[i + 2] : null;
            int n = 2 + (bp != null ? 1 : 0) + (ap != null ? 1 : 0);
            float[] ys = new float[n];
            int j = 0;
            if (bp != null) ys[j++] = bp.v[a];
            ys[j++] = k[i].v[a];
            ys[j++] = k[i + 1].v[a];
            if (ap != null) ys[j] = ap.v[a];
            float tt = (al + (bp != null ? 1 : 0)) / (n - 1);
            float p = (n - 1) * tt;
            int ip = (int) Math.floor(p);
            float w = p - ip;
            if (ip >= n - 1) { ip = n - 2; w = 1f; }
            float p0 = ys[ip == 0 ? 0 : ip - 1], p1 = ys[ip], p2 = ys[Math.min(n - 1, ip + 1)], p3 = ys[Math.min(n - 1, ip + 2)];
            float v0 = (p2 - p0) * 0.5f, v1 = (p3 - p1) * 0.5f, w2 = w * w, w3 = w2 * w;
            return (2 * p1 - 2 * p2 + v0 + v1) * w3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * w2 + v0 * w + p1;
        }

        // curva bezier com as alcas do Blockbench: acha o ponto da curva no tempo t
        private static float bezier(Key b, Key f, int a, float t) {
            float gap = f.t - b.t;
            float x0 = b.t, x1 = b.t + Mth.clamp(b.rt[a], 0, gap), x2 = f.t + Mth.clamp(f.lt[a], -gap, 0), x3 = f.t;
            float y0 = b.v[a], y1 = b.v[a] + b.rv[a], y2 = f.v[a] + f.lv[a], y3 = f.v[a];
            float lo = 0, hi = 1, s = 0.5f;
            for (int it = 0; it < 24; it++) {
                s = (lo + hi) * 0.5f;
                if (cubic(x0, x1, x2, x3, s) < t) lo = s; else hi = s;
            }
            return cubic(y0, y1, y2, y3, s);
        }

        private static float cubic(float p0, float p1, float p2, float p3, float s) {
            float u = 1 - s;
            return u * u * u * p0 + 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s * p3;
        }
    }

    public static final class Anim {
        final String name; final float length; final float[] hits; final float strikeA, strikeB;
        final Map<String, Channel> rot = new HashMap<>(), pos = new HashMap<>(), scale = new HashMap<>();
        Anim(String name, float length, float[] hits, float strikeA, float strikeB) {
            this.name = name; this.length = length; this.hits = hits; this.strikeA = strikeA; this.strikeB = strikeB;
        }
        public float length() { return length; }
        boolean striking(float t) { return t >= strikeA && t <= strikeB; }
        float[] rot(String bone, float t) { Channel c = rot.get(bone); return c == null ? new float[3] : c.at(t); }
        float[] pos(String bone, float t) { Channel c = pos.get(bone); return c == null ? new float[3] : c.at(t); }
        float[] scale(String bone, float t) { Channel c = scale.get(bone); return c == null ? new float[]{1, 1, 1} : c.at(t); }
    }

    // ------------------------------------------------------------------ leitura (uma vez; de novo no F3+T)

    private static final Map<String, Map<String, Anim>> CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener) rm -> CACHE.clear());
    }

    private static Map<String, Anim> load(String weapon) {
        Map<String, Anim> got = CACHE.get(weapon);
        if (got != null) return got;
        Map<String, Anim> out = new HashMap<>();
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "combat_anims/" + weapon + ".json");
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> en : root.entrySet()) out.put(en.getKey(), parse(en.getKey(), en.getValue().getAsJsonObject()));
                }
            }
        } catch (Exception ex) {
            BinosVanillaMod.LOGGER.error("Animacoes do Blockbench ({}) com erro: {}", rl, ex.toString());
        }
        CACHE.put(weapon, out);
        return out;
    }

    private static Anim parse(String name, JsonObject o) {
        JsonArray h = o.getAsJsonArray("hits"), s = o.getAsJsonArray("strike");
        float[] hits = new float[h.size()];
        for (int i = 0; i < hits.length; i++) hits[i] = h.get(i).getAsFloat();
        Anim a = new Anim(name, o.get("length").getAsFloat(), hits, s.get(0).getAsFloat(), s.get(1).getAsFloat());
        for (Map.Entry<String, JsonElement> b : o.getAsJsonObject("bones").entrySet()) {
            JsonObject ch = b.getValue().getAsJsonObject();
            for (String c : new String[]{"rotation", "position", "scale"}) {
                if (!ch.has(c)) continue;
                JsonArray rows = ch.getAsJsonArray(c);
                Key[] keys = new Key[rows.size()];
                for (int i = 0; i < keys.length; i++) {
                    JsonArray r = rows.get(i).getAsJsonArray();
                    keys[i] = new Key(r.get(0).getAsFloat(), vec(r, 1), r.get(4).getAsInt(), vec(r, 5), vec(r, 8), vec(r, 11), vec(r, 14));
                }
                (c.equals("rotation") ? a.rot : c.equals("position") ? a.pos : a.scale).put(b.getKey(), new Channel(keys));
            }
        }
        return a;
    }

    private static float[] vec(JsonArray r, int i) {
        return new float[]{r.get(i).getAsFloat(), r.get(i + 1).getAsFloat(), r.get(i + 2).getAsFloat()};
    }

    // ------------------------------------------------------------------ aplicar no modelo (terceira pessoa)

    // braco, pernas, tronco e cabeca. w = quanto da animacao entra (0 = pose do jogo, 1 = so a animacao).
    // right = mao principal direita (canhoto: tudo espelhado).
    static void poseModel(HumanoidModel<?> m, Anim a, float t, float w, boolean right) {
        boolean mir = !right;
        part(m.body, a, "body", t, w, mir, 0, 0, 0, false);
        part(m.head, a, "head", t, w, mir, 0, 0, 0, true);   // a cabeca soma (continua olhando para onde voce olha)
        part(right ? m.rightArm : m.leftArm, a, "right_arm", t, w, mir, right ? -5 : 5, 2, 0, false);
        part(right ? m.leftArm : m.rightArm, a, "left_arm", t, w, mir, right ? 5 : -5, 2, 0, false);
        part(right ? m.rightLeg : m.leftLeg, a, "right_leg", t, w, mir, right ? -1.9f : 1.9f, 12, 0, false);
        part(right ? m.leftLeg : m.rightLeg, a, "left_leg", t, w, mir, right ? 1.9f : -1.9f, 12, 0, false);
    }

    private static void part(ModelPart p, Anim a, String bone, float t, float w, boolean mir, float dx, float dy, float dz, boolean add) {
        float[] r = a.rot(bone, t), o = a.pos(bone, t);
        float jx = -r[0] * Mth.DEG_TO_RAD, jy = -r[1] * Mth.DEG_TO_RAD, jz = r[2] * Mth.DEG_TO_RAD;
        float ox = -o[0], oy = -o[1], oz = o[2];
        if (mir) { jy = -jy; jz = -jz; ox = -ox; }
        if (add) {   // cabeca: o jogo refaz so o olhar (x/y) e a altura a cada quadro; giro de lado e x/z nao voltam sozinhos
            p.xRot += jx * w; p.yRot += jy * w; p.zRot = jz * w;
            p.x = ox * w; p.y += oy * w; p.z = oz * w;
        } else {
            p.xRot = Mth.lerp(w, p.xRot, jx); p.yRot = Mth.lerp(w, p.yRot, jy); p.zRot = Mth.lerp(w, p.zRot, jz);
            // posicao: x/z a partir do lugar padrao (o jogo nao refaz x/z do tronco e das pernas); y soma a do jogo (agachar)
            p.x = dx + ox * w; p.y += oy * w; p.z = dz + oz * w;
        }
    }

    // escala do jogador (o jogo desenha o jogador em 0,9375) por pixel do Blockbench
    private static final float PX = 0.9375f / 16f, TUMBLE_Y = 14.4f;

    // corpo inteiro (root + tumble), no espaco do jogador depois de virar para onde o corpo olha (bodyYaw)
    static void poseRoot(PoseStack ps, Anim a, float t, float w, boolean right, float bodyYaw) {
        float[] rp = a.pos("root", t), rr = a.rot("root", t), tp = a.pos("tumble", t), tr = a.rot("tumble", t);
        if (!right) { rp[0] = -rp[0]; rr[1] = -rr[1]; rr[2] = -rr[2]; tp[0] = -tp[0]; tr[1] = -tr[1]; tr[2] = -tr[2]; }
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        ps.translate(rp[0] * PX * w, rp[1] * PX * w, rp[2] * PX * w);
        ps.mulPose(Axis.ZP.rotationDegrees(rr[2] * w));
        ps.mulPose(Axis.YP.rotationDegrees(rr[1] * w));
        ps.mulPose(Axis.XP.rotationDegrees(rr[0] * w));
        ps.translate(tp[0] * PX * w, tp[1] * PX * w + TUMBLE_Y * PX, tp[2] * PX * w);
        ps.mulPose(Axis.ZP.rotationDegrees(tr[2] * w));
        ps.mulPose(Axis.YP.rotationDegrees(tr[1] * w));
        ps.mulPose(Axis.XP.rotationDegrees(tr[0] * w));
        ps.translate(0, -TUMBLE_Y * PX, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-(180f - bodyYaw)));
    }

    // a arma (osso "item"): depois do jogo levar a pose ate a mao. Gira e estica a partir da mao.
    static void poseItem(PoseStack ps, Anim a, float t, float w, boolean right) {
        float[] r = a.rot("item", t), o = a.pos("item", t), s = a.scale("item", t);
        float jx = -r[0] * w, jy = -r[1] * w, jz = r[2] * w, ox = -o[0] * w, oy = -o[1] * w, oz = o[2] * w;
        if (!right) { jy = -jy; jz = -jz; ox = -ox; }
        float px = right ? -1f : 1f, py = 9f;   // ponto da mao no braco (pixels do modelo do jogo)
        ps.translate((px + ox) / 16f, (py + oy) / 16f, oz / 16f);
        ps.mulPose(Axis.ZP.rotationDegrees(jz));
        ps.mulPose(Axis.YP.rotationDegrees(jy));
        ps.mulPose(Axis.XP.rotationDegrees(jx));
        ps.scale(Mth.lerp(w, 1f, s[0]), Mth.lerp(w, 1f, s[1]), Mth.lerp(w, 1f, s[2]));
        ps.translate(-px / 16f, -py / 16f, 0);
    }

    // base e ponta da lamina no mundo (para o rastro), pelas mesmas contas do Blockbench
    static Vec3[] blade(Anim a, float t, boolean right, float bodyYaw, Vec3 feet) {
        Matrix4f m = new Matrix4f().rotateY((180f - bodyYaw) * Mth.DEG_TO_RAD);
        if (!right) m.scale(-1, 1, 1);
        m.scale(PX);
        bone(m, a, "root", t, 0, 0, 0);
        bone(m, a, "tumble", t, 0, TUMBLE_Y, 0);
        bone(m, a, "right_arm", t, 5, 22, 0);
        bone(m, a, "item", t, 6, 13, 0);
        Vector4f b = m.transform(new Vector4f(6, 13, -5, 1)), tip = m.transform(new Vector4f(6, 13, -17, 1));
        return new Vec3[]{feet.add(b.x, b.y, b.z), feet.add(tip.x, tip.y, tip.z)};
    }

    private static void bone(Matrix4f m, Anim a, String bone, float t, float px, float py, float pz) {
        float[] r = a.rot(bone, t), o = a.pos(bone, t), s = a.scale(bone, t);
        m.translate(px + o[0], py + o[1], pz + o[2])
                .rotateZ(r[2] * Mth.DEG_TO_RAD).rotateY(r[1] * Mth.DEG_TO_RAD).rotateX(r[0] * Mth.DEG_TO_RAD)
                .scale(s[0], s[1], s[2])
                .translate(-px, -py, -pz);
    }
    // ------------------------------------------------------------------ primeira pessoa (a mesma animacao)
    // A camera fica nos olhos; a mao e a arma fazem o MESMO caminho da terceira pessoa, medido a partir do descanso:
    //  - a mao anda o mesmo tanto (em blocos) que anda no modelo, somado a posicao de descanso da primeira pessoa;
    //  - a arma gira o mesmo tanto que gira no modelo (giro do braco + giro do osso item) e estica igual;
    //  - o corpo inteiro (root/tumble/tronco) vira movimento da camera: vai a frente, abaixa, inclina, tomba.

    // mao e arma no descanso (braco com a arma, 18 graus a frente, como o jogo segura)
    private static Matrix4f restItem() {
        return new Matrix4f().translate(5, 22, 0).rotateX(18 * Mth.DEG_TO_RAD).translate(-5, -22, 0);
    }

    private static Matrix4f itemFrame(Anim a, float t) {
        Matrix4f m = new Matrix4f();
        bone(m, a, "right_arm", t, 5, 22, 0);
        bone(m, a, "item", t, 6, 13, 0);
        return m;
    }

    // parametros da pose da primeira pessoa (mesmo formato do MoveAnims): mao + corpo para a camera
    static float[] firstPersonPose(Anim a, float t, float w) {
        float[] v = MoveAnims.REST.clone();
        Vector3f h = itemFrame(a, t).transformPosition(new Vector3f(6, 13, 0)), h0 = restItem().transformPosition(new Vector3f(6, 13, 0));
        // a mao anda menos que no modelo (a camera esta colada nela): o golpe continua na tela
        v[MoveAnims.HX] += (h.x - h0.x) / 16f * 0.6f * w;
        v[MoveAnims.HY] += (h.y - h0.y) / 16f * 0.45f * w;
        v[MoveAnims.HZ] += (h.z - h0.z) / 16f * 0.6f * w;
        float[] rp = a.pos("root", t), rr = a.rot("root", t), tr = a.rot("tumble", t), br = a.rot("body", t);
        v[MoveAnims.TW] = -br[1] * w;
        v[MoveAnims.LN] = -rr[0] / 0.3f * w;
        v[MoveAnims.SP] = -rr[1];
        v[MoveAnims.RL] = -tr[2] * w;
        v[MoveAnims.FL] = -tr[0];
        v[MoveAnims.DX] = -rp[2] / 16f * w;
        v[MoveAnims.HOP] = Math.max(0, rp[1]) / 16f * w;
        v[MoveAnims.CR] = Mth.clamp(-rp[1] / 16f / 0.12f, 0f, 1f) * w;
        return v;
    }

    // giro da arma na primeira pessoa desde o descanso (espaco da camera); canhoto espelhado
    static Quaternionf firstPersonTurn(Anim a, float t, float w, boolean right) {
        Quaternionf q = itemFrame(a, t).getNormalizedRotation(new Quaternionf());
        Quaternionf q0 = restItem().getNormalizedRotation(new Quaternionf());
        Quaternionf d = q.mul(q0.conjugate());
        if (!right) d.set(d.x, -d.y, -d.z, d.w);
        return new Quaternionf().slerp(d, w);
    }

    static float stretch(Anim a, float t, float w) { return Mth.lerp(w, 1f, a.scale("item", t)[2]); }
}
