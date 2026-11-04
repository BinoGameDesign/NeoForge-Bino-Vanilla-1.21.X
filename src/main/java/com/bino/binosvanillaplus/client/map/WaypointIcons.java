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

    public static final String ITEM_PREFIX = "item:";   // icone novo: "item:minecraft:grass_block" (o bloco, com silhueta)

    // o item de um icone de bloco, ou null se for um icone antigo / item que nao existe mais
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.item.Item item(String id) {
        if (id == null || !id.startsWith(ITEM_PREFIX)) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id.substring(ITEM_PREFIX.length()));
        if (rl == null) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl)
                .filter(i -> i != net.minecraft.world.item.Items.AIR).orElse(null);
    }

    public static String valid(String id) {
        if (item(id) != null) return id;
        if ("tree".equals(id)) return "cross";   // nome antigo
        return id != null && IDS.contains(id) ? id : "pin";
    }

    // so o desenho (fundo transparente)
    public static void draw(GuiGraphics g, String id, int x, int y, int size) {
        net.minecraft.world.item.Item it = item(id);
        if (it != null) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(size / 16f, size / 16f, 1f);
            g.renderItem(new net.minecraft.world.item.ItemStack(it), 0, 0);
            g.pose().popPose();
            return;
        }
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/waypoints/" + valid(id) + ".png");
        g.blit(tex, x, y, size, size, 0, 0, 17, 17, 17, 17);
    }

    // quadrado na cor da marcacao + desenho por cima (como no Figma)
    public static void badge(GuiGraphics g, String id, int x, int y, int size, int color) {
        net.minecraft.world.item.Item it = item(id);
        if (it != null) {   // bloco com silhueta na cor da marcacao (um pouco maior, para o bloco ser reconhecivel)
            float s = size * 1.25f, off = (s - size) / 2f;
            BlockIcons.draw(g, it, x - off, y - off, s, 0xFF000000 | color);
            return;
        }
        g.fill(x, y, x + size, y + size, 0xFF000000 | color);
        draw(g, id, x, y, size);
    }
}
