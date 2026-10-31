#version 150

// Terreno dos mapas: vertices em coordenadas relativas (ao jogador no minimapa, ao chunk no mapa-mundi).
in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 LightMat;            // mapa-mundi: projecao da luz (shadow map)
uniform vec3 ShadowOrigin;        // posicao da malha em relacao ao alvo do mapa-mundi
uniform float ShadowNormalOffset; // empurra o ponto na direcao da normal antes de ler a sombra (evita "acne")

out vec4 vColor;
out vec3 vNormal;
out vec3 vPos;
out float vDist;      // distancia ate a camera (neblina)
out vec4 vShadowPos;  // posicao no espaco da luz
out vec3 vRel;        // posicao relativa ao alvo do mapa-mundi

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vNormal = Normal;
    vPos = Position;
    vDist = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vec3 rel = Position + ShadowOrigin;
    vRel = rel;
    vShadowPos = LightMat * vec4(rel + Normal * ShadowNormalOffset, 1.0);
}
