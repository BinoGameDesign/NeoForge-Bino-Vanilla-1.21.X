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

float fbm(vec2 p) {
    float v = 0.0, amp = 0.5;
    for (int i = 0; i < 5; i++) {
        v += amp * vnoise(p);
        p = p * 2.03 + vec2(17.1, 9.2);
        amp *= 0.5;
    }
    return v;
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
        // lua: quadrado claro com "crateras" em pixels; a parte escura da fase fica transparente (mostra o ceu, como no jogo)
        vec3 moon = -sun;
        float km = dot(dir, moon);
        if (km > 0.0) {
            vec2 uv = bodyUV(dir, moon);
            float m = max(abs(uv.x), abs(uv.y));
            float inside = 1.0 - smoothstep(0.054, 0.060, m);
            if (inside > 0.0) {
                vec2 q = clamp(uv / 0.06 * 0.5 + 0.5, 0.0, 0.999);
                vec2 px = floor(q * 8.0);                              // 8x8 "pixels"
                float crater = hash(px + 3.0) > 0.78 ? 0.82 : 1.0;
                // fase: 0 cheia, 4 nova; ilumina uma fatia que diminui e cresce
                float ph = MoonPhase < 4.5 ? MoonPhase / 4.0 : (8.0 - MoonPhase) / 4.0;   // 0 = cheia .. 1 = nova
                float xs = MoonPhase < 4.5 ? q.x : 1.0 - q.x;
                float lit = step(ph, xs);
                vec3 moonCol = vec3(0.88, 0.90, 0.96) * crater;
                col = mix(col, moonCol, inside * lit);
            }
            col += vec3(0.55, 0.65, 1.0) * pow(km, 40.0) * 0.14 * Night;
        }
    }

    // nuvens fofas (estilo Complementary): ruido em camadas, borda macia, lado do sol claro, base sombreada,
    // borda prateada contra o sol; duas camadas (cumulos + uma camada fina mais alta)
    if (CloudY > -1000.0 && abs(dir.y) > 0.003) {
        vec2 wind = vec2(Time * 0.9, Time * 0.25);
        // camada principal
        float t = (CloudY - CamPos.y) / dir.y;
        if (t > 0.0) {
            vec2 p = (CamPos.xz + dir.xz * t + wind) * 0.010;
            float base = fbm(p);
            float cov = smoothstep(0.50, 0.78, base);
            if (cov > 0.0) {
                // "densidade" na direcao do sol: quanto mais nuvem entre o ponto e o sol, mais escuro
                float toward = fbm(p + sun.xz * 0.12);
                float lightAmt = exp(-max(toward - 0.50, 0.0) * 4.5);
                vec3 litCol = mix(vec3(1.0, 0.86, 0.70), vec3(1.0, 0.99, 0.97), smoothstep(0.0, 0.45, sun.y));
                vec3 shadeCol = mix(MapFogColor, SkyColor, 0.35) * 0.78;
                vec3 cc = mix(shadeCol, litCol, lightAmt);
                float silver = pow(max(dot(dir, sun), 0.0), 12.0) * (1.0 - cov) * 1.4;
                cc += litCol * silver;
                cc *= mix(1.0, 0.28, Night);
                float fade = 1.0 - smoothstep(180.0, 900.0, t);
                col = mix(col, cc, cov * 0.92 * fade);
            }
        }
        // camada fina e alta (cirros)
        float t2 = (CloudY + 70.0 - CamPos.y) / dir.y;
        if (t2 > 0.0) {
            vec2 p2 = (CamPos.xz + dir.xz * t2 + wind * 2.0) * vec2(0.0035, 0.012);
            float c2 = smoothstep(0.58, 0.80, fbm(p2)) * 0.35 * (1.0 - smoothstep(300.0, 1400.0, t2));
            col = mix(col, mix(vec3(1.0), MapFogColor, 0.25) * mix(1.0, 0.3, Night), c2);
        }
    }

    fragColor = vec4(col, 1.0) * ColorModulator;
}
