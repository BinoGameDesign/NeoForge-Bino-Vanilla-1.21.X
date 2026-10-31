#version 150

// Fundo dos mapas: um retangulo que cobre a tela inteira (posicoes ja em coordenadas de tela -1..1).
in vec3 Position;

out vec2 ndc;

void main() {
    ndc = Position.xy;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
