package com.bino.binosvanillaplus.client.lookat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// O que o painel vai mostrar: icone, titulo, nome do mod e linhas extras (preenchidas pelos "providers").
public class LookAtInfo {
    public ItemStack icon = ItemStack.EMPTY;
    public Component title = Component.empty();
    public Component modName = Component.empty();
    public final List<Component> lines = new ArrayList<>();
    public float health = -1;      // >= 0 desenha a barra de vida
    public float maxHealth = 0;
    public int level = 0;
    public net.minecraft.world.entity.Entity entity;   // mob: desenha o rostinho (como no minimapa) no lugar do ovo          // > 0 desenha a gema com o nivel do mob ao lado do nome

    public LookAtInfo line(Component c) { lines.add(c); return this; }
}
