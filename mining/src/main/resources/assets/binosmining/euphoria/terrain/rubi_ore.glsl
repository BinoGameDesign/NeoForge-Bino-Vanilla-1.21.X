// Rubi Ore e Rubi Deepslate Ore: so os cristais (pixels avermelhados) brilham e refletem; a pedra da base nao.
if (color.r - color.g > 0.3) {
    emission = 1.5 + color.r * 1.5; // brilho dos cristais (aumente/diminua aqui)
    smoothnessG = 0.6;              // reflexo do sol/lua
    smoothnessD = smoothnessG;      // reflexo na tela
    highlightMult = 1.5;
}
