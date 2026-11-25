package com.bino.binoscombat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

// Ganchos do Combat para o Bino's Vanilla+ (niveis, atributos, faccoes). O Combat funciona sem eles (valores padrao);
// o Vanilla+ preenche em BinosVanillaMod.
public final class CombatHooks {
    private CombatHooks() {}

    public interface PlayerFloat { float get(Player p); }

    public static ToIntFunction<Mob> mobLevelFn = m -> 0;                  // nivel do mob (mobs fortes encadeiam golpes)
    public static Predicate<Entity> extraFighterFn = e -> false;           // mobs de faccao tambem lutam (aviso, parry)
    public static Predicate<Mob> agileMobFn = m -> false;                  // mobs ageis que esquivam mais (rato)
    public static ToIntFunction<Player> vitalityFn = p -> 0;               // Vitalidade: postura maxima do jogador
    public static PlayerFloat postureMultFn = p -> 1f;                     // Forca: dano na postura
    public static PlayerFloat critChanceFn = p -> 0f;                      // Agilidade: chance de critico
    public static PlayerFloat critDamageFn = p -> 1.5f;                    // Forca: tamanho do critico
    public static IntSupplier clientAgilityFn = () -> 0;                   // Agilidade do jogador local (estamina)


    public static int mobLevel(Mob m) { return mobLevelFn.applyAsInt(m); }
    public static boolean extraFighter(Entity e) { return extraFighterFn.test(e); }
    public static boolean agileMob(Mob m) { return agileMobFn.test(m); }
    public static int vitality(Player p) { return vitalityFn.applyAsInt(p); }
    public static float postureMult(Player p) { return postureMultFn.get(p); }
    public static float critChance(Player p) { return critChanceFn.get(p); }
    public static float critDamage(Player p) { return critDamageFn.get(p); }
    public static int clientAgility() { return clientAgilityFn.getAsInt(); }
}
