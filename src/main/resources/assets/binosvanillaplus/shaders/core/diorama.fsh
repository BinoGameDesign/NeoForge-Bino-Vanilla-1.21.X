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

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void main() {
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

    vec3 sunDir = normalize(vec3(0.45, 0.85, 0.30));
    float sun = max(dot(n, sunDir), 0.0);
    float sky = 0.5 + 0.5 * n.y;
    vec3 sunCol = vec3(1.00, 0.92, 0.80);
    vec3 skyCol = vec3(0.62, 0.74, 0.98);
    vec3 light = skyCol * (0.30 + 0.38 * sky) + sunCol * sun * 0.64;
    float depthDark = clamp(1.0 + vPos.y * 0.016, 0.60, 1.08);
    vec3 col = base * light * depthDark * 1.12;

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = clamp(mix(vec3(luma), col, 1.14), 0.0, 1.0);

    float d = length(vPos.xz) / Radius;
    float fade = 1.0 - smoothstep(0.72, 1.0, d);
    vec3 bg = vec3(0.043, 0.059, 0.078);
    col = mix(bg, col, 0.25 + 0.75 * fade);

    fragColor = vec4(col, vColor.a) * ColorModulator;
}
