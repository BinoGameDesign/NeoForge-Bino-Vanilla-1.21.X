package com.bino.binoscombat.client.combat;

import com.bino.binoscombat.BinosCombat;
import com.bino.binoscombat.combat.BbTiming;
import com.bino.binoscombat.combat.CombatMove;
import com.bino.binosmovement.client.anim.BbAnims;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Golpes animados no BLOCKBENCH (o motor fica no mod Movement: BbAnims). Aqui: qual animacao cada golpe toca
// (assets/binoscombat/combat_anims/<arma>.json, nomes em BbTiming) e a pose dos bracos em primeira pessoa.
// Golpe sem animacao do Blockbench continua com a animacao antiga (MoveAnims).
public final class CombatAnims {
    private CombatAnims() {}

    public static BbAnims.Anim forMove(CombatMove m, boolean axe) {
        String n = BbTiming.name(m);
        if (n == null) return null;
        return BbAnims.load(ResourceLocation.fromNamespaceAndPath(BinosCombat.MOD_ID, "combat_anims/" + (axe ? "axe" : "sword") + ".json")).get(n);
    }

    // mao e arma no descanso (braco com a arma, 18 graus a frente, como o jogo segura)
    private static Matrix4f restItem() {
        return new Matrix4f().translate(5, 22, 0).rotateX(18 * Mth.DEG_TO_RAD).translate(-5, -22, 0);
    }

    private static Matrix4f itemFrame(BbAnims.Sample a) {
        Matrix4f m = new Matrix4f();
        BbAnims.bone(m, a, "right_arm", 5, 22, 0);
        BbAnims.bone(m, a, "item", 6, 13, 0);
        return m;
    }

    // parametros da pose da primeira pessoa (mesmo formato do MoveAnims): mao + corpo para a camera.
    // Com a camera que acompanha o corpo (Movement: HeadCam) ligada, o corpo inteiro ja vai para a camera de verdade;
    // aqui ficam so a mao e a torcao do tronco.
    static float[] firstPersonPose(BbAnims.Sample a, float w, boolean headCam) {
        float[] v = MoveAnims.REST.clone();
        Vector3f h = itemFrame(a).transformPosition(new Vector3f(6, 13, 0)), h0 = restItem().transformPosition(new Vector3f(6, 13, 0));
        // a mao anda menos que no modelo (a camera esta colada nela): o golpe continua na tela
        v[MoveAnims.HX] += (h.x - h0.x) / 16f * 0.6f * w;
        v[MoveAnims.HY] += (h.y - h0.y) / 16f * 0.45f * w;
        v[MoveAnims.HZ] += (h.z - h0.z) / 16f * 0.6f * w;
        float[] rp = a.pos("root"), rr = a.rot("root"), tr = a.rot("tumble"), br = a.rot("body");
        v[MoveAnims.TW] = -br[1] * w;
        if (headCam) return v;
        // inclinacao do root: so ate 45 graus vira "inclinar"; o que passar disso (cambalhota, como no juizo) e' giro
        float lean = Mth.clamp(Mth.wrapDegrees(rr[0]), -45f, 45f);
        v[MoveAnims.LN] = -lean / 0.3f * w;
        v[MoveAnims.SP] = -rr[1];
        v[MoveAnims.RL] = -tr[2] * w;
        v[MoveAnims.FL] = -tr[0] - (rr[0] - lean);
        v[MoveAnims.DX] = -rp[2] / 16f * w;
        v[MoveAnims.HOP] = Math.max(0, rp[1]) / 16f * w;
        v[MoveAnims.CR] = Mth.clamp(-rp[1] / 16f / 0.12f, 0f, 1f) * w;
        return v;
    }
}
