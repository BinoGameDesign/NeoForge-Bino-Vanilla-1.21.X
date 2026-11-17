package com.bino.binosvanillaplus.util;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

// Modelo global de armadura do Bino's Vanilla+.
//  1) Curva: ate 20 pontos de armadura efetiva = igual ao vanilla (x/25).
//     Acima disso a reducao continua subindo com retorno decrescente, tendendo a 99%.
//     Assim o +10 (armadura x2) nao e desperdicado no teto de 20 do vanilla.
//  2) Teto de hit: so para quem veste o set mais forte do jogo (armadura >= CAP_START_ARMOR).
//     Nenhum golpe de mob tira mais que CAP_FRACTION da vida maxima, EXCETO dano da tag
//     binosvanillaplus:boss_special (o ataque especial, escapavel, do boss final).
//     Entre CAP_START_ARMOR e CAP_FULL_ARMOR o teto vai entrando em rampa (sem degrau).
public final class ArmorModel {
    private ArmorModel() {}

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final float SOFT_START = 20f;      // acima disso a curva e suave
    public static final float SOFT_HALF = 20f;       // a cada 20 pontos acima, metade da distancia ate o limite
    public static final float SOFT_TOP = 0.99f;      // reducao maxima possivel (99%)
    public static final float CAP_START_ARMOR = 40f; // abaixo disso: sem teto (netherite +10 = 40)
    public static final float CAP_FULL_ARMOR = 48f;  // a partir daqui: teto cheio (enderita +10 = 48)
    public static final float CAP_FRACTION = 0.70f;  // teto = 70% da vida maxima

    public static final TagKey<DamageType> BOSS_SPECIAL =
            TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "boss_special"));

    // Recebe a "armadura efetiva" x (o numero que o vanilla dividiria por 25) e devolve o equivalente suavizado
    public static float soften(float x) {
        if (x <= SOFT_START) return x;
        float over = x - SOFT_START;
        float r = 0.8f + (SOFT_TOP - 0.8f) * (1f - (float) Math.pow(2.0, -over / SOFT_HALF));
        return r * 25f;
    }

    // Teto de dano por golpe para um jogador com essa armadura, ou -1 se nao ha teto
    public static float hitCap(float armor, float maxHealth, DamageSource source) {
        if (armor <= CAP_START_ARMOR) return -1f;
        if (source.getEntity() == null && source.getDirectEntity() == null) return -1f;   // queda, lava, fome etc. nao contam
        if (source.is(BOSS_SPECIAL) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return -1f;
        float t = Math.min(1f, (armor - CAP_START_ARMOR) / (CAP_FULL_ARMOR - CAP_START_ARMOR));
        return maxHealth * (1f - (1f - CAP_FRACTION) * t);
    }
}
