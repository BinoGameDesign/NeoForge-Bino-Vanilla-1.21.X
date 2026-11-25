package com.bino.binosmining;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

// Pontos onde OUTROS mods do conjunto (Bino's Vanilla+) podem ligar recursos extras no Mining. Sem eles instalados,
// o Mining usa o padrao daqui e funciona sozinho.
public final class MiningHooks {
    private MiningHooks() {}

    // nivel de um mob (niveis de mobs do Vanilla+); 0 = o mob nao tem nivel. Usado na curva da armadura.
    public static ToIntFunction<LivingEntity> mobLevel = e -> 0;

    // o jogador desviou do golpe com o conjunto de rubi (o Vanilla+ mostra "Miss" em cima dele)
    public static Consumer<Player> dodged = p -> {};
}
