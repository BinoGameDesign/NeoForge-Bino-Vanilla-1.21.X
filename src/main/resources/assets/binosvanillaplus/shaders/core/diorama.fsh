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
    // cor do sol: quente perto do horizonte (amanhecer/entardecer), neutra ao meio-dia
    float h = clamp(sunDir.y, 0.0, 1.0);
    vec3 sunCol = mix(vec3(1.00, 0.62, 0.35), vec3(1.00, 0.95, 0.85), smoothstep(0.05, 0.6, h));
    vec3 skyCol = mix(vec3(0.55, 0.62, 0.95), vec3(0.72, 0.84, 1.00), DayLight);
    vec3 light = skyCol * (0.50 + 0.34 * sky) * DayLight + sunCol * sun * 0.62 * DayLight + vec3(0.10);
    float depthDark = clamp(1.0 + vPos.y * 0.010, 0.78, 1.10);
    vec3 col = base * light * depthDark * 1.22;

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = clamp(mix(vec3(luma), col, 1.30), 0.0, 1.0);

    float d = length(vPos.xz) / Radius;
    float fade = 1.0 - smoothstep(0.72, 1.0, d);
    vec3 bg = vec3(0.043, 0.059, 0.078);
    col = mix(bg, col, 0.25 + 0.75 * fade);

    fragColor = vec4(col, vColor.a) * ColorModulator;
}
