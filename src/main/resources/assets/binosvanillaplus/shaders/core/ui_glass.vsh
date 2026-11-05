#version 150
// Painel de vidro: um retangulo na tela; o fragment faz o resto pela posicao do pixel
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
