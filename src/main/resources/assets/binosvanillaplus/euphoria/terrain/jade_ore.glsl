// Cristais (pixels azulados/esverdeados) brilham e refletem um pouco; a pedra da base nao.
if (max(color.g, color.b) - color.r > 0.25) {
    emission = 0.5;
    smoothnessG = 0.3;
    smoothnessD = smoothnessG;
    highlightMult = 1.0;
}
