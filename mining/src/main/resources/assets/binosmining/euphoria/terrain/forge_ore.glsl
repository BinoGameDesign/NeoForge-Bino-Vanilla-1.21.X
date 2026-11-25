// Cristais (pixels bege/ambar) brilham e refletem um pouco; a pedra cinza da base nao.
if (color.r - color.b > 0.15) {
    emission = 0.5;
    smoothnessG = 0.3;
    smoothnessD = smoothnessG;
    highlightMult = 1.0;
}
