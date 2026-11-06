package com.bino.binosvanillaplus.faction;

import net.minecraft.ChatFormatting;

// As faccoes do jogador. NONE = ainda nao escolheu (a tela de escolha abre ao entrar).
public enum Faction {
    NONE("none", ChatFormatting.GRAY),
    VILLAGER("villager", ChatFormatting.GREEN),
    PILLAGER("pillager", ChatFormatting.DARK_RED),
    WANDERER("wanderer", ChatFormatting.GOLD);

    public final String id;
    public final ChatFormatting color;

    Faction(String id, ChatFormatting color) { this.id = id; this.color = color; }

    public static Faction of(int i) { return i >= 0 && i < values().length ? values()[i] : NONE; }

    public static Faction byId(String id) {
        for (Faction f : values()) if (f.id.equals(id)) return f;
        return null;
    }

    // Villager x Pillager sao rivais (nome vermelho, aliados de um atacam o outro). Errante nao e' rival de ninguem.
    public boolean rival(Faction o) {
        return (this == VILLAGER && o == PILLAGER) || (this == PILLAGER && o == VILLAGER);
    }

    public String langKey() { return "faction.binosvanillaplus." + id; }
}
