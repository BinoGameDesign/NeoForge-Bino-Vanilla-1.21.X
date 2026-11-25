#version 150

// Mascara do contorno da forja: o item com cor lisa (rgb = cor do item, alfa = faixa +4/+6/+8/+10).
// Usa as mesmas matrizes do desenho do item, entao a silhueta fica exatamente em cima dele.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
