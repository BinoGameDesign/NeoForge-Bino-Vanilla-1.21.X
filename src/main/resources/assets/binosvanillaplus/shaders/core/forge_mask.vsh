#version 150

// Mascara do contorno da forja: posicoes ja em coordenadas de tela (-1..1, calculadas no Java),
// cor lisa (rgb = cor do item, alfa = faixa +4/+6/+8/+10)
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
