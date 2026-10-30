#version 150

// Iluminacao propria do minimapa (escrita do zero, inspirada na ideia de luz solar + luz do ceu dos shader packs):
//  - luz direcional do "sol" por face (topo claro, laterais mais escuras)
//  - luz do ceu: faces viradas para cima recebem mais
//  - escurecimento leve com a profundidade (cavidades e vales ficam mais escuros)
//  - borda da maquete se dissolve na cor do fundo (vinheta circular)
in vec4 vColor;
in vec3 vNormal;
in vec3 vPos;

uniform vec4 ColorModulator;
uniform float Radius;

out vec4 fragColor;

void main() {
    vec3 n = normalize(vNormal);
    vec3 sunDir = normalize(vec3(0.45, 0.85, 0.30));
    float sun = max(dot(n, sunDir), 0.0);
    float sky = 0.5 + 0.5 * n.y;
    float light = 0.34 + 0.50 * sun + 0.16 * sky;

    float depthDark = clamp(1.0 + vPos.y * 0.018, 0.62, 1.06);
    vec3 col = vColor.rgb * light * depthDark;

    // leve contraste/saturacao, como nos shader packs
    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(luma), col, 1.12);

    float d = length(vPos.xz) / Radius;
    float fade = 1.0 - smoothstep(0.70, 1.0, d);
    vec3 bg = vec3(0.043, 0.059, 0.078);
    col = mix(bg, col, 0.30 + 0.70 * fade);

    fragColor = vec4(col, vColor.a) * ColorModulator;
}
