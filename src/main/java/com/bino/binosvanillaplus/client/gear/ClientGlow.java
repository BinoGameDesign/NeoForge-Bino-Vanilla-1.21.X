package com.bino.binosvanillaplus.client.gear;

import net.minecraft.Util;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

// Contorno branco (o mesmo do efeito "brilhando" do jogo, visto atraves de paredes) que so ESTE jogador ve,
// por um tempo curto. Usado pelo apito (silhueta de quem apitou) e pelo sonar (criaturas detectadas).
public final class ClientGlow {
    private ClientGlow() {}

    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    public static void add(int entityId, long ms) {
        UNTIL.merge(entityId, Util.getMillis() + ms, Math::max);
    }

    public static boolean has(Entity e) {
        if (UNTIL.isEmpty()) return false;
        Long t = UNTIL.get(e.getId());
        if (t == null) return false;
        if (t < Util.getMillis()) { UNTIL.remove(e.getId()); return false; }
        return true;
    }

    public static void clear() { UNTIL.clear(); }
}
