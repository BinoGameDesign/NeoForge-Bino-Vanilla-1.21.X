package com.bino.binosvanillaplus.client.atmosphere;

// "Cara" da neblina de um lugar (bioma, caverna, dimensao). Duas camadas, como neblina de verdade:
//  - NEVOA ALTA (haze): o ar ao longe, igual em toda altura; e' ela que da a cor do horizonte e da neblina do jogo
//  - NEVOA BAIXA (mist): uma camada que deita no chao/vales e fica mais rala conforme sobe (exponencial pela altura)
// Cores em 0xRRGGBB. Densidades por bloco (0.0016 = ~35% de neblina a 256 blocos).
public final class FogLook {
    public final int day, night, mist, sky;   // sky = -1: nao mexe no ceu do jogo
    public final float skyStrength;
    public final float haze;                   // densidade da nevoa alta
    public final float mistDensity;            // densidade da nevoa baixa no nivel da base
    public final float mistHeight;             // a nevoa baixa cai para ~37% a cada 'mistHeight' blocos acima da base
    public final float mistBase;               // base: relativa ao chao em volta (ou Y absoluto se absBase)
    public final boolean absBase;
    public final float start;                  // distancia sem neblina perto do jogador

    private FogLook(Builder b) {
        day = b.day; night = b.night; mist = b.mist; sky = b.sky; skyStrength = b.skyStrength;
        haze = b.haze; mistDensity = b.mistDensity; mistHeight = b.mistHeight; mistBase = b.mistBase;
        absBase = b.absBase; start = b.start;
    }

    public static Builder of(int day) { return new Builder(day); }

    public static final class Builder {
        int day, night = -1, mist = -1, sky = -1;
        float skyStrength = 0f, haze = 0.0016f, mistDensity = 0.0035f, mistHeight = 9f, mistBase = -2f, start = 4f;
        boolean absBase = false;

        Builder(int day) { this.day = day; }

        public Builder night(int c) { night = c; return this; }
        public Builder mist(int c) { mist = c; return this; }
        public Builder sky(int c, float strength) { sky = c; skyStrength = strength; return this; }
        public Builder haze(float d) { haze = d; return this; }
        public Builder low(float density, float height) { mistDensity = density; mistHeight = height; return this; }
        public Builder base(float offset) { mistBase = offset; return this; }
        public Builder absBase(float y) { mistBase = y; absBase = true; return this; }
        public Builder start(float s) { start = s; return this; }

        public FogLook build() {
            if (night < 0) night = mixRgb(0x0A0E1C, scaleRgb(day, 0.10f), 0.35f);
            if (mist < 0) mist = mixRgb(day, 0xFFFFFF, 0.45f);
            return new FogLook(this);
        }
    }

    static int mixRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int scaleRgb(int a, float k) {
        int r = Math.min(255, (int) (((a >> 16) & 255) * k));
        int g = Math.min(255, (int) (((a >> 8) & 255) * k));
        int b = Math.min(255, (int) ((a & 255) * k));
        return (r << 16) | (g << 8) | b;
    }
}
