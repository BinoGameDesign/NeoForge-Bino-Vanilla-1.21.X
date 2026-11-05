package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

// Fases dos minerios enquanto sao minerados: os cristais vao caindo (a -> b -> c -> d) conforme a rachadura avanca.
// Tudo no cliente: o jogo ja avisa o progresso de quebra (o mesmo da rachadura, inclusive de outros jogadores);
// aqui guardamos a fase por bloco e mandamos redesenhar o bloco. Soltou o botao = volta ao normal (igual a rachadura).
public final class MiningStages {
    private MiningStages() {}

    // nomes dos blocos (e prefixo dos modelos) que tem fases
    public static final List<String> ORE_NAMES = List.of("rubi_ore", "rubi_deepslate_ore", "sapphire_ore", "sapphire_deepslate_ore",
            "jade_ore", "jade_deepslate_ore", "diamond_ore", "deepslate_diamond_ore", "forge_ore");

    private static final Map<Long, Integer> STAGE = new ConcurrentHashMap<>();       // lido pelas threads que montam os chunks
    private static final Map<Integer, long[]> BREAKERS = new HashMap<>();            // quem esta quebrando -> {pos, progresso}

    public static int stageAt(BlockPos pos) {
        Integer s = STAGE.get(pos.asLong());
        return s == null ? 0 : s;
    }

    // progresso da rachadura 0..9 -> fase. a: 0-1, b: 2-4, c: 5-7, d: 8-9
    private static int stageOf(int progress) {
        return progress >= 8 ? 3 : progress >= 5 ? 2 : progress >= 2 ? 1 : 0;
    }

    // Chamado pelo mixin em LevelRenderer.destroyBlockProgress (progresso fora de 0..9 = parou de quebrar)
    public static void onProgress(int breaker, BlockPos pos, int progress) {
        long[] old = BREAKERS.remove(breaker);
        if (progress >= 0 && progress < 10) BREAKERS.put(breaker, new long[]{pos.asLong(), progress});
        if (old != null && old[0] != pos.asLong()) refresh(old[0]);
        refresh(pos.asLong());
    }

    private static void refresh(long key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos pos = BlockPos.of(key);
        BlockState state = mc.level.getBlockState(pos);
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (!ORE_NAMES.contains(name)) { STAGE.remove(key); return; }
        int stage = 0;
        for (long[] b : BREAKERS.values()) if (b[0] == key) stage = Math.max(stage, stageOf((int) b[1]));
        int before = stageAt(pos);
        if (stage == before) return;
        if (stage == 0) STAGE.remove(key); else STAGE.put(key, stage);
        mc.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        if (stage > before) shatter(mc, pos, name, stage - before);
    }

    // cristais caindo + som de cristal quebrando
    private static void shatter(Minecraft mc, BlockPos pos, String ore, int steps) {
        String gem = ore.contains("rubi") ? "rubi" : ore.contains("sapphire") ? "sapphire" : ore.contains("jade") ? "jade"
                : ore.contains("diamond") ? "diamond" : "forge";
        TextureAtlasSprite sprite = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "block/" + gem + "_ore_crystal"));
        RandomSource r = mc.level.random;
        int n = 7 * steps + r.nextInt(4);
        for (int i = 0; i < n; i++) {
            // um ponto numa das faces do bloco, um pouco para fora
            int face = r.nextInt(6);
            double u = r.nextDouble(), v = r.nextDouble();
            double x = 0.5, y = 0.5, z = 0.5;
            switch (face) {
                case 0 -> { x = -0.05; y = u; z = v; }
                case 1 -> { x = 1.05; y = u; z = v; }
                case 2 -> { y = 1.05; x = u; z = v; }
                case 3 -> { y = 0.1; x = u; z = v; }
                case 4 -> { z = -0.05; x = u; y = v; }
                default -> { z = 1.05; x = u; y = v; }
            }
            double vx = (x - 0.5) * 0.12 + (r.nextDouble() - 0.5) * 0.04;
            double vz = (z - 0.5) * 0.12 + (r.nextDouble() - 0.5) * 0.04;
            double vy = 0.06 + r.nextDouble() * 0.08;
            mc.particleEngine.add(new CrystalShardParticle(mc.level, pos.getX() + x, pos.getY() + y, pos.getZ() + z, vx, vy, vz, sprite));
        }
        mc.level.playLocalSound(pos, SoundEvents.SMALL_AMETHYST_BUD_BREAK, SoundSource.BLOCKS, 0.8f, 0.9f + r.nextFloat() * 0.3f, false);
    }
}
