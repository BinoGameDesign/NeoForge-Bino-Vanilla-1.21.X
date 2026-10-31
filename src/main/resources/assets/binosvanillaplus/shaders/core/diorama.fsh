#version 150

// Shader do terreno dos mapas (escrito do zero, inspirado no visual de shader packs como o Complementary).
// Dois modos:
//  - Quality < 0.5 (minimapa): textura procedural, luz de sol/ceu simples, sombras ja vem prontas nas cores (CPU)
//  - Quality >= 0.5 (mapa-mundi): sombra real (shadow map com PCF suave), luz do sol/lua com cor pela altura,
//    luz do ceu + luz rebatida, agua com reflexo do ceu e brilho do sol, tonemap filmico e neblina com dispersao do sol
in vec2 vUV;
in vec4 vColor;
in vec3 vNormal;
in vec3 vPos;
in float vDist;
in vec4 vShadowPos;
in vec3 vRel;

uniform sampler2D ShadowMap;
uniform sampler2D Sampler0;    // atlas de blocos do jogo (texturas de verdade)

uniform vec4 ColorModulator;
uniform float Radius;
uniform vec3 Origin;
uniform float Detail;
uniform vec3 SunDir;      // direcao da luz principal (sol de dia, lua de noite)
uniform vec3 SunVis;      // direcao do sol de verdade
uniform float DayLight;   // 1 = dia, ~0.55 = noite
uniform float Night;      // 0 = dia, 1 = noite
uniform float Exposure;   // 1.0 minimapa; menor no mapa-mundi
uniform vec3 MapFogColor;    // cor da neblina (a mesma do ceu do mapa / do jogo)
uniform float MapFogStart;   // neblina pela distancia da camera (mapa-mundi)
uniform float MapFogEnd;
uniform vec3 Offset;      // origem da malha em relacao ao centro do mapa (neblina circular do minimapa)
uniform float Quality;    // 0 = minimapa, 1 = mapa-mundi
uniform float ShadowOn;   // 1 = ha shadow map valido
uniform float ShadowTexel;   // tamanho de 1 texel do shadow map (em uv)
uniform float ShadowBias;    // vies de profundidade
uniform vec3 CamRel;      // camera em relacao ao alvo (mapa-mundi)
uniform vec3 SkyAmbient;  // cor do ceu (luz ambiente)

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

// tonemap filmico (curva tipo ACES): contraste bonito e realces que nao estouram
vec3 filmic(vec3 x) {
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

const vec2 POISSON[12] = vec2[12](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893),
    vec2(0.507, 0.064), vec2(0.896, 0.412), vec2(-0.322, -0.933), vec2(-0.792, -0.598));

// 1 = iluminado, 0 = na sombra (borda suave)
float shadowAt(float ndl) {
    if (ShadowOn < 0.5) return 1.0;
    vec3 sp = vShadowPos.xyz / vShadowPos.w * 0.5 + 0.5;
    if (sp.x <= 0.0 || sp.x >= 1.0 || sp.y <= 0.0 || sp.y >= 1.0 || sp.z >= 1.0) return 1.0;
    float bias = ShadowBias * (1.0 + 2.5 * (1.0 - ndl));
    float rot = hash(floor(vRel * 8.0)) * 6.2832;          // gira o disco por pixel: penumbra sem "degraus"
    mat2 R = mat2(cos(rot), -sin(rot), sin(rot), cos(rot));
    float lit = 0.0;
    for (int i = 0; i < 12; i++) {
        float d = textureLod(ShadowMap, sp.xy + R * POISSON[i] * ShadowTexel * 1.8, 0.0).r;
        lit += (sp.z - bias > d) ? 0.0 : 1.0;
    }
    lit /= 12.0;
    float edge = smoothstep(0.0, 0.06, min(min(sp.x, 1.0 - sp.x), min(sp.y, 1.0 - sp.y)));
    return mix(1.0, lit, edge);
}

void main() {
    if (Detail < 0.5) {               // ponteiro e feixes: cor pura, sem iluminacao
        fragColor = vec4(vColor.rgb, vColor.a) * ColorModulator;
        return;
    }
    vec3 n = normalize(vNormal);
    bool water = vColor.a < 0.95;                       // agua (translucida)
    bool overlay = !water && vColor.a < 0.999;          // camada por cima (ex.: borda verde da grama)
    bool textured = vUV.x >= 0.0;
    vec4 tex = vec4(1.0);
    if (textured) {
        // le a textura sem "vazar" para a textura vizinha do atlas (de longe, o jogo usa versoes menores da
        // textura e a borda pegava cor da vizinha = listras/camada clara nas emendas): prende dentro da celula 16x16
        vec2 asz = vec2(textureSize(Sampler0, 0));
        vec2 px = vUV * asz;
        vec2 cell = floor(px / 16.0) * 16.0;
        vec2 ddx = dFdx(px), ddy = dFdy(px);
        float lod = clamp(0.5 * log2(max(max(dot(ddx, ddx), dot(ddy, ddy)), 1e-6)), 0.0, 4.0);
        float m = exp2(floor(lod + 0.5)) * 0.5 + 0.01;
        px = clamp(px, cell + m, cell + 16.0 - m);
        tex = textureLod(Sampler0, px / asz, lod);
        if (tex.a < 0.1) {
            if (overlay || water) discard;
            tex.rgb = vec3(0.30);                       // buracos das folhas viram folhagem escura (sem ver o chao)
        }
    }
    vec3 base = tex.rgb * vColor.rgb;

    // textura procedural (so sem textura de verdade) + contorno suave nas arestas
    {
        vec3 inside = vPos + Origin - n * 0.01;
        float noise = textured ? 0.0 : (hash(floor(inside * 4.0)) - 0.5) * (water ? 0.06 : 0.14);
        vec3 f = fract(inside);
        vec3 d = min(f, 1.0 - f);
        vec3 an = abs(n);
        float e = an.y > 0.5 ? min(d.x, d.z) : (an.x > 0.5 ? min(d.y, d.z) : min(d.x, d.y));
        float edge = mix(water ? 0.94 : (textured ? 0.88 : 0.80), 1.0, smoothstep(0.0, 0.07, e));
        base *= (1.0 + noise) * edge;
    }

    vec3 L = normalize(SunDir);
    float h = clamp(normalize(SunVis).y, 0.0, 1.0);
    vec3 sunCol = mix(vec3(1.00, 0.62, 0.36), vec3(1.00, 0.95, 0.86), smoothstep(0.02, 0.55, h));
    vec3 moonCol = vec3(0.55, 0.66, 1.00);
    vec3 lightCol = mix(sunCol, moonCol, Night);
    vec3 col;

    if (Quality < 0.5) {
        // ---------------- minimapa (leve)
        float sun = max(dot(n, L), 0.0);
        float sky = 0.5 + 0.5 * n.y;
        vec3 skyCol = mix(vec3(0.60, 0.66, 0.95), vec3(0.74, 0.85, 1.00), DayLight);
        vec3 light = skyCol * (0.46 + 0.12 * sky) * DayLight + lightCol * sun * 0.50 * DayLight + vec3(0.05);
        float depthDark = clamp(1.0 + vPos.y * 0.008, 0.80, 1.06);
        col = base * light * depthDark * Exposure;
        if (water && n.y > 0.5) {
            col = mix(col, skyCol * 0.85 * DayLight, 0.10);
            col += lightCol * pow(max(dot(n, L), 0.0), 6.0) * 0.06 * DayLight;
        }
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        col = max(mix(vec3(luma), col, 1.15), 0.0);
        col = mix(col, 0.80 + 0.20 * tanh((col - 0.80) / 0.20), step(0.80, col));
    } else {
        // ---------------- mapa-mundi (qualidade)
        vec3 V = normalize(vRel - CamRel);            // da camera para o ponto
        float ndl = max(dot(n, L), 0.0);
        float sh = ndl > 0.0 ? shadowAt(ndl) : 0.0;
        // luz do sol/lua (lua bem mais fraca)
        float lightPow = mix(1.12, 0.30, Night) * smoothstep(-0.02, 0.10, L.y);
        vec3 direct = lightCol * ndl * sh * lightPow;
        // luz do ceu: mais forte em cima, azulada; mais fraca a noite
        // luz do ceu: menos azul (sombras azuladas demais deixavam laterais "azuis")
        vec3 skyAmb = mix(SkyAmbient, vec3(0.80, 0.85, 0.95), 0.55);
        vec3 ambient = skyAmb * (0.25 + 0.20 * (0.5 + 0.5 * n.y)) * mix(1.0, 0.30, Night);
        // luz rebatida do chao nas laterais (quente)
        ambient += vec3(0.42, 0.36, 0.26) * 0.16 * (1.0 - abs(n.y)) * (1.0 - Night) * (0.4 + 0.6 * h);
        // partes baixas (vales, encostas) um pouco mais escuras
        float occl = clamp(1.0 + (vRel.y) * 0.006, 0.82, 1.05);
        col = base * (direct + ambient * occl + vec3(0.02));

        if (water) {
            float facing = max(dot(-V, n), 0.0);
            float fres = pow(1.0 - facing, 4.0);
            vec3 refl = mix(MapFogColor, SkyAmbient, 0.6) * mix(1.0, 0.25, Night);
            col = mix(col, refl, 0.15 + 0.55 * fres);
            vec3 r = reflect(V, n);
            float spec = pow(max(dot(r, L), 0.0), 140.0) * sh;
            col += lightCol * spec * mix(3.0, 0.8, Night);
        }

        col *= Exposure * 0.98;
        col = filmic(col);
        // vibracao: satura mais as cores pouco saturadas
        float luma = dot(col, vec3(0.2126, 0.7152, 0.0722));
        float sat = max(max(col.r, col.g), col.b) - min(min(col.r, col.g), col.b);
        col = clamp(mix(vec3(luma), col, 1.22 + 0.30 * (1.0 - sat)), 0.0, 1.0);

        // neblina com dispersao: olhando para o sol a neblina fica dourada
        float mie = pow(max(dot(V, normalize(SunVis)), 0.0), 6.0) * (1.0 - Night);
        vec3 fogC = MapFogColor + sunCol * mie * 0.30;
        float fog = smoothstep(MapFogStart, MapFogEnd, vDist);
        fog = max(fog, (1.0 - exp(-max(vDist - MapFogStart * 0.5, 0.0) * 0.0012)) * 0.18);   // leve bruma ao longe
        col = mix(col, fogC, clamp(fog, 0.0, 1.0));
        float a = water ? mix(vColor.a, 1.0, 0.35 * pow(1.0 - max(dot(-V, n), 0.0), 3.0)) : 1.0;
        fragColor = vec4(col, a) * ColorModulator;
        return;
    }

    // neblina: circular nas bordas do minimapa (Radius) e pela distancia (FogStart..FogEnd)
    float d = length(vPos.xz + Offset.xz) / Radius;
    float fog = max(smoothstep(0.80, 1.0, d), smoothstep(MapFogStart, MapFogEnd, vDist));
    col = mix(col, MapFogColor, fog);
    fragColor = vec4(col, water ? vColor.a : 1.0) * ColorModulator;
}
