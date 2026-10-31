package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// Icones das marcacoes (assets/binosvanillaplus/textures/gui/waypoints/<id>.png, 17x17, desenhados no Figma).
// O PNG tem so o desenho branco com contorno; o fundo e' pintado na cor da marcacao (badge).
public class WaypointIcons {
    public static final List<String> IDS = List.of("pin", "x", "house", "mine", "danger", "star", "flag", "heart",
            "gem", "bed", "chest", "portal", "cross", "sword", "skull", "cave");
    public static final List<String> NAMES = List.of("Local", "Marca", "Casa", "Mina", "Perigo", "Estrela", "Bandeira", "Coração",
            "Gema", "Cama", "Baú", "Portal", "Cruz", "Batalha", "Morte", "Montanha");

    public static String valid(String id) {
        if ("tree".equals(id)) return "cross";   // nome antigo
        return id != null && IDS.contains(id) ? id : "pin";
    }

    // so o desenho (fundo transparente)
    public static void draw(GuiGraphics g, String id, int x, int y, int size) {
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/waypoints/" + valid(id) + ".png");
        g.blit(tex, x, y, size, size, 0, 0, 17, 17, 17, 17);
    }

    // quadrado na cor da marcacao + desenho por cima (como no Figma)
    public static void badge(GuiGraphics g, String id, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + size, 0xFF000000 | color);
        draw(g, id, x, y, size);
    }
}
