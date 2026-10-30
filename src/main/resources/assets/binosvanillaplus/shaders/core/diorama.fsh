#version 150

// Shader do minimapa / mapa-mundi 3D (escrito do zero; a ideia de luz solar quente + luz de ceu fria,
// contraste e saturacao suaves e' inspirada em shader packs como o Complementary).
//  - textura procedural: cada bloco tem 4x4 "pixels" com variacao de brilho, estavel no mundo (usa Origin)
//  - contorno suave nas arestas dos blocos
//  - luz do sol (quente) por face + luz do ceu (fria) + escurecimento com a profundidade
//  - borda do diorama se dissolve no fundo
in vec4 vColor;
in vec3 vNormal;
in vec3 vPos;

uniform vec4 ColorModulator;
uniform float Radius;
uniform vec3 Origin;
uniform float Detail;
uniform vec3 SunDir;      // direcao do sol (ou da lua a noite), em coordenadas do mundo
uniform float DayLight;   // 1 = dia, ~0.45 = noite
uniform float Exposure;   // 1.0 minimapa; menor no mapa-mundi (tela grande estoura mais facil)

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void main() {
    if (Detail < 0.5) {               // ponteiro: branco puro, sem iluminacao
        fragColor = vec4(vColor.rgb, vColor.a) * ColorModulator;
        return;
    }
    vec3 n = normalize(vNormal);
    vec3 base = vColor.rgb;

    if (Detail > 0.5) {
        vec3 inside = vPos + Origin - n * 0.01;
        float noise = (hash(floor(inside * 4.0)) - 0.5) * 0.14;
        vec3 f = fract(inside);
        vec3 d = min(f, 1.0 - f);
        vec3 an = abs(n);
        float e = an.y > 0.5 ? min(d.x, d.z) : (an.x > 0.5 ? min(d.y, d.z) : min(d.x, d.y));
        float edge = mix(0.78, 1.0, smoothstep(0.0, 0.07, e));
        base *= (1.0 + noise) * edge;
    }

    vec3 sunDir = normalize(SunDir);
    float sun = max(dot(n, sunDir), 0.0);
    float sky = 0.5 + 0.5 * n.y;
    // cor do sol: quente perto do horizonte (amanhecer/entardecer), quase neutra ao meio-dia
    float h = clamp(sunDir.y, 0.0, 1.0);
    vec3 sunCol = mix(vec3(1.00, 0.72, 0.48), vec3(1.00, 0.96, 0.88), smoothstep(0.05, 0.6, h));
    vec3 skyCol = mix(vec3(0.60, 0.66, 0.95), vec3(0.74, 0.85, 1.00), DayLight);
    // luz total ~1.0 numa face iluminada (nao passa de 1 para nao estourar); ceu enche a sombra
    vec3 light = skyCol * (0.36 + 0.20 * sky) * DayLight + sunCol * sun * 0.50 * DayLight + vec3(0.05);
    float depthDark = clamp(1.0 + vPos.y * 0.008, 0.80, 1.06);
    vec3 col = base * light * depthDark * Exposure;

    // agua (alpha < 1): reflexo suave do ceu e brilho do sol
    if (vColor.a < 0.99 && n.y > 0.5) {
        col = mix(col, skyCol * 0.85 * DayLight, 0.10);
        col += sunCol * pow(max(dot(n, sunDir), 0.0), 6.0) * 0.06 * DayLight;
    }

    // saturacao leve e "ombro" suave: valores altos comprimem em vez de estourar para branco
    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = max(mix(vec3(luma), col, 1.15), 0.0);
    col = mix(col, 0.80 + 0.20 * tanh((col - 0.80) / 0.20), step(0.80, col));

    float d = length(vPos.xz) / Radius;
    float fade = 1.0 - smoothstep(0.80, 1.0, d);
    vec3 bg = vec3(0.043, 0.059, 0.078);
    col = mix(bg, col, 0.25 + 0.75 * fade);

    fragColor = vec4(col, vColor.a) * ColorModulator;
}
