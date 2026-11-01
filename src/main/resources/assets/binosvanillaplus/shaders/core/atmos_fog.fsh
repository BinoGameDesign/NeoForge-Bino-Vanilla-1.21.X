#version 150

// Neblina do ambiente em CAMADAS (Bino's Vanilla+, escrita do zero).
// Para cada pixel: reconstroi a posicao do que esta na tela pela profundidade e soma duas nevoas ao longo do raio:
//  - nevoa ALTA (ar distante): mesma densidade em todo lugar, mas afinando com a altura (o ceu la em cima fica limpo)
//  - nevoa BAIXA: deita na base (chao/vales/mar de lava) e cai exponencialmente para cima; integrada em passos ao longo
//    do raio, com manchas de ruido 3D que andam com o vento -> vales enevoados, morros saindo da neblina, nada duro
// Saida: cor * opacidade (pre-multiplicada), aplicada pelo atmos_blend.
in vec2 ndc;

uniform sampler2D DepthTex;
uniform mat4 InvProjView;
uniform vec3 AtmCam;       // camera no mundo (x,z reduzidos para nao perder precisao)
uniform vec3 AtmFog;       // cor da nevoa alta
uniform vec3 AtmMist;      // cor da nevoa baixa
uniform vec3 AtmSunDir;    // sol (ou lua a noite)
uniform vec3 AtmSunCol;    // brilho do sol atravessando a neblina (0 em caverna)
uniform vec4 AtmHaze;      // x densidade, y comeco (blocos), z opacidade maxima, w distancia usada para o ceu
uniform vec4 AtmLayer;     // x densidade da nevoa baixa, y base (Y), z altura, w quantidade de manchas
uniform float AtmHazeH;    // altura em que a nevoa alta afina (enorme = igual em toda altura)
uniform float AtmMistFar;  // ate onde integrar a nevoa baixa
uniform float AtmTime;     // segundos
uniform vec3 AtmLightPos;  // luz colorida do pet (relativa a camera)
uniform vec3 AtmLightCol;  // cor * intensidade (0 = sem luz)
uniform float AtmLightR;   // raio da luz em blocos

out vec4 fragColor;

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float vnoise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(hash3(i), hash3(i + vec3(1.0, 0.0, 0.0)), f.x);
    float b = mix(hash3(i + vec3(0.0, 1.0, 0.0)), hash3(i + vec3(1.0, 1.0, 0.0)), f.x);
    float c = mix(hash3(i + vec3(0.0, 0.0, 1.0)), hash3(i + vec3(1.0, 0.0, 1.0)), f.x);
    float d = mix(hash3(i + vec3(0.0, 1.0, 1.0)), hash3(i + vec3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}

// ruido de gradiente intercalado (espalha os passos e tira o "degrau" das faixas)
float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

// integral da densidade exponencial pela altura ao longo de um trecho que sobe/desce 'dy'
float heightFactor(float dy, float h) {
    float x = dy / h;
    if (abs(x) < 1e-3) return 1.0;
    return clamp((1.0 - exp(-x)) / x, 0.0, 4.0);
}

void main() {
    float depth = texelFetch(DepthTex, ivec2(gl_FragCoord.xy), 0).r;
    bool sky = depth >= 0.99999;
    vec3 dir;
    float dist;
    if (sky) {
        vec4 b = InvProjView * vec4(ndc, 1.0, 1.0);
        dir = normalize(b.xyz / b.w);
        dist = AtmHaze.w;
    } else {
        vec4 p4 = InvProjView * vec4(ndc, depth * 2.0 - 1.0, 1.0);
        vec3 p = p4.xyz / p4.w;
        dist = length(p);
        dir = p / max(dist, 1e-4);
    }
    float jit = ign(gl_FragCoord.xy);

    // luz colorida do pet: soma um brilho da cor dele nas superficies em volta (sem shaderpack a luz do jogo e' branca)
    vec3 glow = vec3(0.0);
    if (!sky && dot(AtmLightCol, AtmLightCol) > 0.0) {
        vec3 pp = dir * dist;
        float ld = length(pp - AtmLightPos) / AtmLightR;
        glow = AtmLightCol * pow(clamp(1.0 - ld, 0.0, 1.0), 2.2);
    }

    // nevoa alta
    float L = max(dist - AtmHaze.y, 0.0);
    float tauH = AtmHaze.x * L * heightFactor(dir.y * L, AtmHazeH);
    if (sky) tauH *= 0.8;   // o ceu do jogo ja desbota no horizonte

    // nevoa baixa (10 passos, mais densos perto da camera)
    float tauM = 0.0;
    if (AtmLayer.x > 1e-6) {
        float lm = min(dist, AtmMistFar);
        float prev = 0.0;
        for (int i = 1; i <= 10; i++) {
            float e = float(i) / 10.0;
            e = e * e * lm;
            float dt = e - prev;
            float t = prev + dt * jit;
            prev = e;
            vec3 wp = AtmCam + dir * t;
            float hgt = wp.y - AtmLayer.y;
            float d = exp(-clamp(hgt, -AtmLayer.z, 400.0) / AtmLayer.z);
            vec3 q = vec3(wp.x * 0.021 + AtmTime * 0.045, wp.y * 0.05 - AtmTime * 0.01, wp.z * 0.021 + AtmTime * 0.018);
            float n = vnoise3(q) * 0.65 + vnoise3(q * 2.3 + 7.1) * 0.35;
            d *= mix(1.0, 0.2 + 1.6 * n, AtmLayer.w);
            d *= smoothstep(0.0, AtmHaze.y + 2.0, t);
            tauM += d * dt;
        }
        tauM *= AtmLayer.x;
    }

    float tau = tauH + tauM;
    float a = min(1.0 - exp(-tau), AtmHaze.z);
    if (a < 0.002) {
        fragColor = vec4(glow, 0.0);
        return;
    }
    vec3 col = (AtmFog * tauH + AtmMist * tauM) / max(tau, 1e-6);
    // o sol atravessando a neblina: brilho quente olhando para ele
    float mu = max(dot(dir, normalize(AtmSunDir)), 0.0);
    col += AtmSunCol * (pow(mu, 10.0) * 0.55 + pow(mu, 3.0) * 0.16);
    col = clamp(col + (jit - 0.5) / 255.0, 0.0, 1.0);
    fragColor = vec4(col * a + glow * (1.0 - a), a);
}
