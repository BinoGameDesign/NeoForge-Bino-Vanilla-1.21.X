package com.bino.binosvanillaplus.pocket;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// Constantes e contas da Dimensao Portatil. E UMA dimensao so (data/binosvanillaplus/dimension/pocket.json), e cada
// jogador tem a sua sala num "lote" proprio, um do lado do outro ao longo do eixo X (a cada CELL blocos).
// Tudo que o jogador constroi la fica salvo no mundo junto com a dimensao (baus, camas, bancadas...).
public final class PocketDimension {
    private PocketDimension() {}

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pocket"));

    public static final int CELL = 128;       // distancia entre as salas (cabe sala de ate 120 blocos)
    public static final int FLOOR_Y = 64;     // 1o bloco LIVRE da sala; o chao inquebravel fica em FLOOR_Y - 1
    public static final int BASE_HALF = 4;    // nivel 0 = 9x9 (meia-largura 4)

    public static int halfFor(int level) { return BASE_HALF + level; }
    public static int sizeFor(int level) { return halfFor(level) * 2 + 1; }       // 9, 11, 13...
    public static int heightFor(int level) { return 7 + level; }                  // altura livre acima do chao
    public static int centerX(int index) { return index * CELL; }
    public static int indexAt(double x) { return Math.floorDiv((int) Math.floor(x) + CELL / 2, CELL); }

    public static boolean is(Level level) { return level.dimension().equals(LEVEL); }

    @Nullable
    public static ServerLevel get(MinecraftServer server) { return server.getLevel(LEVEL); }

    // onde o jogador aparece ao entrar: logo em frente a porta de saida (que fica na parede do fundo, ao norte)
    public static Vec3 entrance(int index, int level) {
        return new Vec3(centerX(index) + 0.5, FLOOR_Y + 1, -halfFor(level) + 1.5);   // cai 1 bloco; nunca nasce dentro do chao do jogador
    }
}
