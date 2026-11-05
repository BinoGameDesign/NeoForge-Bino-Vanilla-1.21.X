#version 150

// Luz do personagem do inventario:
//  - luz principal de cima/esquerda (quente), sombra suave do lado oposto
//  - luz de recorte (rim) fria nas faces que olham para a direita/baixo
//  - leve oclusao: faces de baixo mais escuras
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 normal;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0) * vertexColor;
    if (tex.a < 0.1) discard;
    vec3 n = normalize(normal);
    vec2 keyDir = normalize(vec2(-0.6, -0.8));          // na tela: esquerda e cima (y da tela cresce para baixo)
    float front = abs(n.z);                              // face virada para quem olha
    float key = max(0.0, dot(n.xy, keyDir));
    float light = 0.42 + front * 0.50 + key * 0.42;
    float under = max(0.0, n.y);                         // face de baixo
    light -= under * 0.12;
    vec3 col = tex.rgb * light * vec3(1.03, 1.0, 0.96);  // luz levemente quente
    float rim = max(0.0, dot(n.xy, -keyDir)) * (1.0 - front);
    col += vec3(0.55, 0.72, 0.95) * rim * 0.22;          // recorte frio do lado da sombra
    fragColor = vec4(min(col, vec3(1.0)), tex.a) * ColorModulator;
}
