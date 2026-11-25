package com.bino.binosmining.client;

import com.bino.binosmining.BinosMining;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

// Escolhe, pela POSICAO do bloco, qual dos 8 modelos desenhar. Nao guarda nada: a mesma posicao sempre da o mesmo modelo.
// Tambem deixa os CRISTAIS (textura rubi_ore_crystal) brilhando no maximo, mesmo no escuro; a pedra da base nao brilha.
public class RubiOreVariantModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<Integer> VARIANT = new ModelProperty<>();
    public static final ModelProperty<Integer> STAGE = new ModelProperty<>();   // 0..3: fases do bloco sendo minerado (MiningStages)

    // Brilho dos cristais, de 0 (nenhum) a 15 (maximo). Mude aqui para deixar mais fraco.
    private static final int CRYSTAL_GLOW = 15;

    private static final IQuadTransformer GLOW = QuadTransformers.settingEmissivity(CRYSTAL_GLOW);

    private final BakedModel[] variants;
    private final BakedModel[][] stageModels;   // [variante][fase] (null = usa o modelo inteiro da variante)
    // [variante * 4 + fase] -> por lado (0..5 = Direction.ordinal(), 6 = sem lado), ja com o brilho aplicado.
    // Montado so quando o bloco aparece pela primeira vez (as fases de mineracao quase nunca aparecem, e eram 3/4 do trabalho).
    private final AtomicReferenceArray<List<BakedQuad>[]> glowing;

    public RubiOreVariantModel(BakedModel[] variants) { this(variants, null); }

    // stages[v][1..3] = modelos das fases b, c e d da variante v (null = ainda nao feita: usa o modelo inteiro)
    public RubiOreVariantModel(BakedModel[] variants, @Nullable BakedModel[][] stages) {
        super(variants[0]);
        this.variants = variants;
        this.stageModels = stages;
        this.glowing = new AtomicReferenceArray<>(variants.length * 4);
    }

    // quads da variante v na fase st, com os cristais acesos (montados uma vez; as threads que montam chunks leem sem trava)
    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] glowing(int v, int st) {
        int idx = v * 4 + st;
        List<BakedQuad>[] sides = glowing.get(idx);
        if (sides != null) return sides;
        synchronized (this) {
            sides = glowing.get(idx);
            if (sides != null) return sides;
            BakedModel m = st == 0 || stageModels == null || stageModels[v] == null || stageModels[v][st] == null ? variants[v] : stageModels[v][st];
            sides = new List[7];
            RandomSource rand = RandomSource.create(42L);
            for (int s = 0; s < 7; s++) {
                rand.setSeed(42L);
                List<BakedQuad> quads = m.getQuads(null, s == 6 ? null : DIRECTIONS[s], rand, ModelData.EMPTY, null);
                List<BakedQuad> out = new java.util.ArrayList<>(quads.size());
                for (BakedQuad q : quads) out.add(isCrystal(q.getSprite().contents().name()) ? GLOW.process(q) : q);
                sides[s] = List.copyOf(out);
            }
            glowing.set(idx, sides);
            return sides;
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    // Qualquer textura do nosso mod chamada block/<gema>_ore_crystal (rubi, safira, jade, diamante...) brilha.
    private static boolean isCrystal(ResourceLocation name) {
        return name.getNamespace().equals(BinosMining.MOD_ID) && name.getPath().endsWith("_ore_crystal");
    }

    // Formula: f = (a(x) + b(y) + c(z)) mod 8, onde cada termo = 4*(paridade da coordenada) + hash(0..3).
    // Dois blocos vizinhos (lado/cima/baixo) diferem em UMA coordenada, entao a paridade dessa coordenada muda
    // e o termo muda de 1 a 7 (4 +- 3) => o resultado mod 8 e' SEMPRE diferente. Impossivel repetir ao lado.
    public static int variantFor(BlockPos pos) {
        return (term(pos.getX(), 0x9E3779B1) + term(pos.getY(), 0x85EBCA6B) + term(pos.getZ(), 0xC2B2AE35)) & 7;
    }

    private static int term(int c, int salt) {
        int h = (c >> 1) * 0x27D4EB2F + salt;
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 12; h *= 0x297A2D39; h ^= h >>> 15;
        return 4 * (c & 1) + (h & 3);
    }

    private int index(ModelData data) {
        Integer v = data.get(VARIANT);
        return v == null ? 0 : v;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return modelData.derive().with(VARIANT, variantFor(pos)).with(STAGE, MiningStages.stageAt(pos)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        Integer st = data.get(STAGE);
        return glowing(index(data), st == null ? 0 : st)[side == null ? 6 : side.ordinal()];
    }

    // O bloco emite luz, e por padrao isso desliga a iluminacao suave (AO). Forcamos ligada para a pedra nao ficar "chapada".
    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return TriState.TRUE;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return variants[index(data)].getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return variants[index(data)].getParticleIcon(data);
    }
}
