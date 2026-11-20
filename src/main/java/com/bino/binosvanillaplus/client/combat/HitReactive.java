package com.bino.binosvanillaplus.client.combat;

// Para mobs NOVOS com reacao ao golpe propria: o modelo implementa esta interface e mexe nas pecas como quiser.
// E' chamado depois do setupAnim, logo antes de desenhar. O setupAnim do modelo precisa refazer a pose a cada quadro
// (os modelos do jogo e os gerados do Blockbench ja fazem: resetPose/animacoes), senao a mudanca acumula.
//   k     = forca da reacao agora (0..1; sobe rapido no impacto e volta devagar)
//   side  = de que lado veio o golpe (-1 esquerda .. +1 direita, em relacao ao mob)
//   front = golpe de frente (+1) ou por tras (-1)
// Devolve true para NAO aplicar a reacao generica (cabeca/bracos/cauda pelo nome das pecas).
// Sem implementar nada, todo modelo ja recebe a reacao generica (HitReact) + o tranco/inclinacao do corpo inteiro.
public interface HitReactive {
    boolean hitReact(float k, float side, float front);
}
