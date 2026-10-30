package com.bino.binosvanillaplus.map;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Regra central: o minimapa e o mapa-mundi so existem se o jogador tem um mapa (vazio ou preenchido) na hotbar/mao secundaria
public class MapGate {
    public static boolean isMap(ItemStack s) {
        return s.is(Items.MAP) || s.is(Items.FILLED_MAP);
    }

    public static boolean hasMap(Player p) {
        for (int i = 0; i < 9; i++) if (isMap(p.getInventory().getItem(i))) return true;
        return isMap(p.getOffhandItem());
    }
}
