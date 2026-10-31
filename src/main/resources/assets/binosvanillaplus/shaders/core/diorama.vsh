#version 150

// Minimapa diorama: vertices do terreno em coordenadas relativas ao jogador.
in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec3 vNormal;
out vec3 vPos;
out float vDist;      // distancia ate a camera (neblina)

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vNormal = Normal;
    vPos = Position;
    vDist = length((ModelViewMat * vec4(Position, 1.0)).xyz);
}
