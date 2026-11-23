package com.bino.binosvanillaplus.client.recipes;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class LookupKeys {
    private static final String CATEGORY = "key.categories.binosvanillaplus";

    // so nas telas (inventario, receitas): nao briga com a tecla Defender (R) no jogo
    public static final KeyMapping SHOW_RECIPE = new KeyMapping("key.binosvanillaplus.show_recipe",
            net.neoforged.neoforge.client.settings.KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping SHOW_USES = new KeyMapping("key.binosvanillaplus.show_uses",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);
    public static final KeyMapping BOOKMARK = new KeyMapping("key.binosvanillaplus.bookmark",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping OPEN = new KeyMapping("key.binosvanillaplus.open_recipes",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);
}
