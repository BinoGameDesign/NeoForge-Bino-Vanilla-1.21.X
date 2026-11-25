package com.bino.binoscombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

// Ganchos do Combat que so existem no cliente (o Vanilla+ preenche em ExampleModClient)
public final class CombatClientHooks {
    private CombatClientHooks() {}

    // primeira pessoa dos golpes: a armadura do peito nos bracos (o Vanilla+ desenha a armadura 3D do Mining)
    public interface HandArmor {
        void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, boolean right, PlayerModel<AbstractClientPlayer> model);
    }
    public static HandArmor handArmor = (ps, buf, light, player, right, model) -> {};
}
