#version 150

// Ceu dos mapas (escrito do zero): para cada pixel calcula a direcao do raio da camera e pinta
//  - acima do horizonte: degrade da cor da neblina (horizonte) para a cor do ceu do Minecraft
//  - abaixo: a propria neblina, um pouco mais escura (o "fora do mapa" some na fog)
//  - brilho do sol na direcao dele
//  - nuvens em blocos de 12x12 (como as do jogo), na altura das nuvens da dimensao, andando com o tempo
in vec2 ndc;

uniform mat4 InvProjView;
uniform vec4 ColorModulator;
uniform vec3 SkyColor;
uniform vec3 FogColor;
uniform vec3 SunDir;
uniform float DayLight;
uniform vec3 CamPos;      // posicao da camera no mundo
uniform float CloudY;     // altura das nuvens (< -1000 = dimensao sem nuvens)
uniform float Time;       // segundos

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.1030));
    p += dot(p, p.yx + 33.33);
    return fract((p.x + p.y) * p.x);
}

// ruido suave entre os cantos de uma grade (avaliado por celula -> nuvens em blocos)
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + vec2(1.0, 0.0)), c = hash(i + vec2(0.0, 1.0)), d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec4 a = InvProjView * vec4(ndc, -1.0, 1.0);
    vec4 b = InvProjView * vec4(ndc, 1.0, 1.0);
    vec3 dir = normalize(b.xyz / b.w - a.xyz / a.w);

    float up = dir.y;
    vec3 col;
    if (up >= 0.0) col = mix(FogColor, SkyColor, smoothstep(0.0, 0.40, up));
    else col = FogColor;   // igual a neblina do terreno: a borda do mapa some sem emenda

    // sol (ou lua, a noite: SunDir aponta para ela) - nucleo pequeno + halo largo
    float sd = max(dot(dir, normalize(SunDir)), 0.0);
    vec3 glowCol = mix(vec3(0.75, 0.80, 1.0), vec3(1.0, 0.88, 0.65), DayLight);
    col += glowCol * (pow(sd, 400.0) * 0.9 + pow(sd, 10.0) * 0.10) * (0.35 + 0.65 * DayLight);

    // nuvens
    if (CloudY > -1000.0 && abs(dir.y) > 0.005) {
        float t = (CloudY - CamPos.y) / dir.y;
        if (t > 0.0) {
            vec2 p = CamPos.xz + dir.xz * t;
            p.x += Time * 0.6;
            vec2 cell = floor(p / 12.0);
            float n = vnoise(cell * 0.21) * 0.68 + vnoise(cell * 0.53 + 17.0) * 0.32;
            float c = smoothstep(0.54, 0.58, n);
            float fade = 1.0 - smoothstep(120.0, 520.0, t);
            vec3 cloudCol = mix(FogColor, vec3(1.0), 0.70) * (0.50 + 0.50 * DayLight);
            col = mix(col, cloudCol, c * 0.85 * fade);
        }
    }

    fragColor = vec4(col, 1.0) * ColorModulator;
}
