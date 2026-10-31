#version 150

// Passo de sombra do mapa-mundi: desenha o terreno visto do sol so para gravar a profundidade.
in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
