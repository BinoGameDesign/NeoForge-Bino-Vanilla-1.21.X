package com.bino.binosvanillaplus.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// a busca do criativo (para as teclas do Receitas+ nao abrirem enquanto voce digita)
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeSearchAccessor {
    @Accessor("searchBox")
    EditBox binosvanillaplus$searchBox();
}
