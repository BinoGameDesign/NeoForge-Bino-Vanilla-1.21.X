#version 150

// Luz do personagem do inventario (bem mais marcada que a do jogo):
//  - frente bem iluminada, luz principal de cima/esquerda
//  - lado direito/baixo em sombra, com recorte de luz fria na borda
//  - topo das pecas com um brilho leve
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
    float front = abs(n.z);
    float key = max(0.0, dot(n.xy, keyDir));
    float shade = max(0.0, dot(n.xy, -keyDir));
    float light = 0.52 + front * 0.50 + key * 0.38;               // faces de baixo/sombra nunca ficam pretas (minimo 0.52)
    vec3 col = tex.rgb * light * vec3(1.06, 1.0, 0.92);           // luz quente
    col += vec3(1.0, 0.95, 0.85) * pow(key, 3.0) * 0.10;           // brilho no topo/esquerda
    col += vec3(0.45, 0.65, 1.0) * shade * (1.0 - front) * 0.20;   // recorte frio no lado da sombra
    fragColor = vec4(clamp(col, 0.0, 1.0), tex.a) * ColorModulator;
}
