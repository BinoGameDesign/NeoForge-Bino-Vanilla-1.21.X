#version 150

// Ceu dos mapas (escrito do zero). Para cada pixel calcula a direcao do raio da camera e pinta:
//  - degrade horizonte (cor da neblina) -> zenite (cor do ceu do Minecraft), com faixa quente no por/nascer do sol
//  - SOL e LUA quadrados como no jogo, no mesmo lugar do ceu do Minecraft (lua do lado oposto, com a fase atual)
//  - estrelas a noite
//  - nuvens em blocos de 12x12, com lado iluminado e sombreado, andando com o tempo
//  - abaixo do horizonte: a propria neblina (a borda do mapa some sem emenda)
in vec2 ndc;

uniform mat4 InvProjView;
uniform vec4 ColorModulator;
uniform vec3 SkyColor;
uniform vec3 MapFogColor;
uniform vec3 SunVis;      // direcao do sol (a lua e' -SunVis)
uniform float DayLight;
uniform float Night;      // 0 dia .. 1 noite
uniform float MoonPhase;  // 0 = cheia ... 4 = nova ... 7
uniform vec3 CamPos;      // posicao da camera no mundo
uniform float CloudY;     // altura das nuvens (< -1000 = sem nuvens)
uniform float Time;       // segundos
uniform float HasSky;     // 0 = Nether/End (sem sol, lua, estrelas)

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.1030));
    p += dot(p, p.yx + 33.33);
    return fract((p.x + p.y) * p.x);
}

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + vec2(1.0, 0.0)), c = hash(i + vec2(0.0, 1.0)), d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// coordenadas (u,v) do raio no "plano" de um astro (sol/lua), para desenhar o quadrado
vec2 bodyUV(vec3 dir, vec3 body) {
    vec3 side = normalize(cross(body, vec3(0.0, 0.0, 1.0)));   // o sol anda no plano X-Y (leste-oeste)
    vec3 up = cross(side, body);
    float k = dot(dir, body);
    return vec2(dot(dir, side), dot(dir, up)) / max(k, 1e-4);
}

float cloudAt(vec2 p) {
    vec2 cell = floor(p / 12.0);
    float n = vnoise(cell * 0.21) * 0.68 + vnoise(cell * 0.53 + 17.0) * 0.32;
    return smoothstep(0.545, 0.565, n);
}

void main() {
    vec4 a = InvProjView * vec4(ndc, -1.0, 1.0);
    vec4 b = InvProjView * vec4(ndc, 1.0, 1.0);
    vec3 dir = normalize(b.xyz / b.w - a.xyz / a.w);
    vec3 sun = normalize(SunVis);
    float up = dir.y;

    vec3 col;
    if (up >= 0.0) {
        col = mix(MapFogColor, SkyColor, smoothstep(0.0, 0.42, up));
        // faixa quente perto do horizonte, do lado do sol, no amanhecer/entardecer
        float low = (1.0 - smoothstep(0.0, 0.40, abs(sun.y))) * HasSky;
        float side = pow(max(dot(normalize(dir.xz + 1e-5), normalize(sun.xz + 1e-5)), 0.0), 2.0);
        float band = low * side * (1.0 - smoothstep(0.0, 0.35, up));
        col = mix(col, vec3(1.0, 0.52, 0.28), band * 0.55);
    } else {
        col = MapFogColor;   // igual a neblina do terreno: a borda do mapa some sem emenda
    }

    if (HasSky > 0.5) {
        // estrelas
        if (up > 0.0 && Night > 0.01) {
            vec3 g = floor(dir * 220.0);
            float s = step(0.9975, hash3(g));
            float tw = 0.6 + 0.4 * sin(Time * 3.0 + hash3(g + 7.0) * 40.0);
            col += vec3(0.85, 0.90, 1.0) * s * tw * Night * smoothstep(0.0, 0.25, up) * 0.9;
        }
        // sol: quadrado branco-amarelado com halo
        float ks = dot(dir, sun);
        if (ks > 0.0) {
            vec2 uv = bodyUV(dir, sun);
            float m = max(abs(uv.x), abs(uv.y));
            float core = 1.0 - smoothstep(0.075, 0.082, m);
            col = mix(col, vec3(1.0, 0.98, 0.86), core);
            col += vec3(1.0, 0.85, 0.60) * (pow(ks, 60.0) * 0.35 + pow(ks, 8.0) * 0.10) * (1.0 - core);
        }
        // lua: quadrado com "crateras" em pixels e a fase atual
        vec3 moon = -sun;
        float km = dot(dir, moon);
        if (km > 0.0) {
            vec2 uv = bodyUV(dir, moon);
            float m = max(abs(uv.x), abs(uv.y));
            if (m < 0.06) {
                vec2 px = floor((uv / 0.06 * 0.5 + 0.5) * 8.0);   // 8x8 "pixels"
                float crater = hash(px + 3.0) > 0.78 ? 0.80 : 1.0;
                // fase: escurece um lado (0 cheia, 4 nova)
                float ph = MoonPhase < 4.5 ? MoonPhase / 4.0 : (8.0 - MoonPhase) / 4.0;   // 0..1
                float lit = (MoonPhase < 4.5 ? (uv.x / 0.06) : -(uv.x / 0.06)) * 0.5 + 0.5;
                float shade = step(ph, lit) * 0.85 + 0.15;
                col = mix(col, vec3(0.86, 0.88, 0.95) * crater * shade, 1.0 - smoothstep(0.055, 0.06, m));
            }
            col += vec3(0.55, 0.65, 1.0) * pow(km, 40.0) * 0.18 * Night;
        }
    }

    // nuvens
    if (CloudY > -1000.0 && abs(dir.y) > 0.004) {
        float t = (CloudY - CamPos.y) / dir.y;
        if (t > 0.0) {
            vec2 p = CamPos.xz + dir.xz * t;
            p.x += Time * 0.6;
            float c = cloudAt(p);
            if (c > 0.0) {
                // borda do lado do sol mais clara, do lado oposto mais escura (da volume)
                float lit = cloudAt(p - sun.xz * 6.0);
                float shade = mix(1.0, 0.82, lit);
                float fade = 1.0 - smoothstep(140.0, 600.0, t);
                vec3 sunTint = mix(vec3(1.0, 0.80, 0.62), vec3(1.0), smoothstep(0.0, 0.4, sun.y));
                vec3 cloudCol = mix(MapFogColor, vec3(1.0), 0.72) * sunTint * shade * mix(1.0, 0.35, Night);
                col = mix(col, cloudCol, c * 0.88 * fade);
            }
        }
    }

    fragColor = vec4(col, 1.0) * ColorModulator;
}
