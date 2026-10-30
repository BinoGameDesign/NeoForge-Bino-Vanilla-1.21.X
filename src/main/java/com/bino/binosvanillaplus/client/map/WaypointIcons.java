package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// Icones das marcacoes (assets/binosvanillaplus/textures/gui/waypoints/<id>.png, 16x16)
public class WaypointIcons {
    public static final List<String> IDS = List.of("pin", "x", "house", "mine", "danger", "star", "flag", "heart",
            "gem", "bed", "chest", "portal", "tree", "sword", "skull", "cave");
    public static final List<String> NAMES = List.of("Local", "Marca", "Casa", "Mina", "Perigo", "Estrela", "Bandeira", "Coração",
            "Gema", "Cama", "Baú", "Portal", "Árvore/Fazenda", "Batalha", "Morte", "Caverna");

    public static String valid(String id) {
        return id != null && IDS.contains(id) ? id : "pin";
    }

    public static void draw(GuiGraphics g, String id, int x, int y, int size) {
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/waypoints/" + valid(id) + ".png");
        g.blit(tex, x, y, size, size, 0, 0, 16, 16, 16, 16);
    }
}
