package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Cor "de verdade" de cada bloco: media da textura (topo e lateral) multiplicada pela cor do bioma (grama, folhas, agua).
// E' o que permite diferenciar folha de tronco, grama de terra, etc. no minimapa e no mapa-mundi.
public class BlockLooks {
    private static final class Look { int top = 0xFF808080, side = 0xFF808080, tintTop = -1, tintSide = -1; }
    private static final Map<BlockState, Look> CACHE = new HashMap<>();

    // limpa o cache (chamar ao recarregar recursos)
    public static void clear() { CACHE.clear(); }

    public static int top(BlockState st, BlockAndTintGetter level, BlockPos pos) {
        FluidState fs = st.getFluidState();
        if (!fs.isEmpty() && (st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return fluid(fs, level, pos);
        Look l = look(st);
        return tint(l.top, l.tintTop, st, level, pos);
    }

    public static int side(BlockState st, BlockAndTintGetter level, BlockPos pos) {
        FluidState fs = st.getFluidState();
        if (!fs.isEmpty() && (st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return fluid(fs, level, pos);
        Look l = look(st);
        return tint(l.side, l.tintSide, st, level, pos);
    }

    private static int fluid(FluidState fs, BlockAndTintGetter level, BlockPos pos) {
        if (fs.is(FluidTags.LAVA)) return 0xFFE0661A;
        int c = IClientFluidTypeExtensions.of(fs).getTintColor(fs, level, pos);
        return 0xFF000000 | (c & 0xFFFFFF);
    }

    private static int tint(int base, int tintIndex, BlockState st, BlockAndTintGetter level, BlockPos pos) {
        if (tintIndex < 0) return base;
        int c = Minecraft.getInstance().getBlockColors().getColor(st, level, pos, tintIndex);
        if (c == -1) return base;
        int r = ((base >> 16) & 0xFF) * ((c >> 16) & 0xFF) / 255, g = ((base >> 8) & 0xFF) * ((c >> 8) & 0xFF) / 255,
                b = (base & 0xFF) * (c & 0xFF) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static Look look(BlockState st) {
        Look l = CACHE.get(st);
        if (l != null) return l;
        l = new Look();
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(st);
            RandomSource rs = RandomSource.create(42L);
            BakedQuad up = first(model.getQuads(st, Direction.UP, rs));
            BakedQuad side = first(model.getQuads(st, Direction.NORTH, rs));
            BakedQuad any = up != null ? up : first(model.getQuads(st, null, rs));   // plantas em cruz, etc.
            if (any != null) { l.top = average(any.getSprite()); l.tintTop = any.isTinted() ? any.getTintIndex() : -1; }
            BakedQuad s = side != null ? side : any;
            if (s != null) { l.side = average(s.getSprite()); l.tintSide = s.isTinted() ? s.getTintIndex() : -1; }
            else { l.side = l.top; l.tintSide = l.tintTop; }
            // lateral de bloco com topo tingido (ex.: grama): a lateral nao e' tingida, entao fica terra - correto
        } catch (Exception ignored) {
            l.top = l.side = 0xFF808080;
        }
        CACHE.put(st, l);
        return l;
    }

    private static BakedQuad first(List<BakedQuad> quads) { return quads == null || quads.isEmpty() ? null : quads.get(0); }

    private static int average(TextureAtlasSprite sprite) {
        NativeImage img = sprite.contents().getOriginalImage();
        int w = sprite.contents().width(), h = Math.min(sprite.contents().height(), w);   // 1o quadro (texturas animadas sao empilhadas)
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getPixelRGBA(x, y);   // ABGR
                int a = (c >>> 24) & 0xFF;
                if (a < 40) continue;
                r += c & 0xFF; g += (c >> 8) & 0xFF; b += (c >> 16) & 0xFF; n++;
            }
        }
        if (n == 0) return 0xFF808080;
        return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
    }
}
