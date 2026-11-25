package com.bino.binosmovement.client;

import com.bino.binosmovement.client.anim.BbAnims;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Ganchos do Movement para outros mods (o Combat preenche no inicio do cliente). Sem ninguem preencher, o Movement
// funciona sozinho: so a vida no corpo, as animacoes do dia a dia e a camera.
public final class MovementHooks {
    private MovementHooks() {}

    // ---- corpo (terceira pessoa e o corpo visivel da primeira pessoa)

    // pose por cima da vida no corpo (golpes, rolamento...), na ordem em que foram registradas
    public interface BodyPoser { void pose(HumanoidModel<?> m, Player p); }
    public static final List<BodyPoser> POSERS = new ArrayList<>();

    // guarda de combate (arma a frente, joelhos dobrados) parado
    public static Predicate<Player> stanceFn = p -> false;
    // o corpo inteiro esta numa animacao de outro mod (golpe, rolamento): a vida no corpo nao mexe no corpo todo
    public static Predicate<Player> bodyBusyFn = p -> false;

    public static boolean stance(Player p) { return stanceFn.test(p); }
    public static boolean bodyBusy(Player p) { return bodyBusyFn.test(p); }

    // ---- camera

    // animacao de corpo inteiro tocando agora (Blockbench): a camera de primeira pessoa vai junto (HeadCam)
    // headShare = quanto do giro da cabeca da animacao vai para a camera (golpes 0,5; andar 0, senao a tela balanca parado)
    public record BodyFrame(BbAnims.Sample sample, float weight, boolean rightHanded, float headShare) {
        public BodyFrame(BbAnims.Sample sample, float weight, boolean rightHanded) { this(sample, weight, rightHanded, 0.5f); }
    }
    public interface BodySource { BodyFrame frame(Player p, float partialTick); }
    public static final List<BodySource> BODY_SOURCES = new ArrayList<>();

    public static BodyFrame bodyFrame(Player p, float pt) {
        for (BodySource s : BODY_SOURCES) {
            BodyFrame f = s.frame(p, pt);
            if (f != null && f.weight() > 0.001f) return f;
        }
        return null;
    }

    // camera de cinema (ex.: facada pelas costas): posicao x, y, z, yaw, pitch; null = camera normal
    public interface CinemaCam { float[] cam(Player p, float partialTick); }
    public static final List<CinemaCam> CINEMA = new ArrayList<>();

    // primeira pessoa: quanto a camera abaixa e vai a frente (blocos) por coisas que nao sao do Blockbench
    public interface CameraNudge { float[] nudge(Player p, float partialTick); }   // {abaixa, frente}
    public static final List<CameraNudge> NUDGES = new ArrayList<>();

    // a mira (camera no ombro e a mira que segue o alvo quando a camera gira); o Combat troca pela mira nova
    public interface CrosshairDrawer { void draw(GuiGraphics g, int x, int y); }
    public static CrosshairDrawer crosshair = (g, x, y) -> {
        g.fill(x - 4, y, x + 5, y + 1, 0xE0FFFFFF);
        g.fill(x, y - 4, x + 1, y + 5, 0xE0FFFFFF);
    };
}
