package com.bino.binosmining.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.ElderGuardian;

// Armadura estilo World of Warcraft: reducao = A / (A + K), com K crescendo com o NIVEL de quem ataca.
//   A = armadura + 2 x resistencia (toughness)      K = K_BASE + K_PER_LEVEL x nivel do atacante      reducao maxima = MAX_REDUCTION
// Contra mobs de nivel baixo a armadura rende quase como no vanilla; contra mobs nivel 100 ela rende bem menos
// (e o dano deles ja e maior), entao subir armadura/forja/encantos continua valendo ate o fim do jogo.
// Sem teto de 20 pontos: o +10 (armadura e resistencia x2) sempre ajuda, com retorno decrescente.
// Resultado de exemplo (so armadura, sem encantos): ver claude/mining-balanceamento.md
public final class ArmorModel {
    private ArmorModel() {}

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final float TOUGHNESS_WEIGHT = 2f;   // quanto a resistencia vale em pontos de armadura
    public static final float K_BASE = 8f;
    public static final float K_PER_LEVEL = 0.12f;     // nivel 100 => K = 20
    public static final float MAX_REDUCTION = 0.85f;
    public static final int BOSS_LEVEL = 50;           // chefes vanilla nao tem nivel de mob; contam como este

    public static int attackerLevel(DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden || e instanceof ElderGuardian) return BOSS_LEVEL;
        if (e instanceof LivingEntity le) { int lv = com.bino.binosmining.MiningHooks.mobLevel.applyAsInt(le); if (lv > 0) return lv; }
        return 1;
    }

    public static float reduction(float armor, float toughness, int level) {
        float a = armor + TOUGHNESS_WEIGHT * toughness;
        if (a <= 0f) return 0f;
        return Math.min(MAX_REDUCTION, a / (a + K_BASE + K_PER_LEVEL * level));
    }
}
