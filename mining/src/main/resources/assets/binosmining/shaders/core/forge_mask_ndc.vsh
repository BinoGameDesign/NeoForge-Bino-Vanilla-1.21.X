#version 150

// Mascara do contorno da forja, versao "anotada": posicoes ja em coordenadas de tela (-1..1), calculadas no Java
// durante o quadro (mundo, armadura, corpos). Cor lisa: rgb = cor do item, alfa = faixa.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
