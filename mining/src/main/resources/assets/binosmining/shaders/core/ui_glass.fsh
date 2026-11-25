#version 150
// Vidro dos menus (estilo "vidro liquido"):
//  - mostra o fundo desfocado, com uma tinta por cima (escura nos paineis, clara nos botoes)
//  - perto da borda arredondada o fundo e' entortado (refracao), com uma leve separacao de cor
//  - fio de luz na borda: forte em cima/esquerda, fraco embaixo/direita (invertido quando o vidro e' "afundado", nos slots)
//  Mode.z > 0 = sombra suave (so escurece, sem vidro)
uniform sampler2D Backdrop;
uniform vec2 ScreenSize;
uniform vec4 Rect;      // x, y (de baixo), largura, altura - em pixels da tela
uniform float Radius;
uniform vec4 Tint;      // cor, quanto
uniform vec4 Params;    // refracao (px), faixa da borda (px), luz da borda, brilho leitoso
uniform vec4 Mode;      // afundado (0/1), pixels por unidade de GUI, sombra (0..1), espalhamento da sombra (px)
uniform vec4 ColorModulator;
out vec4 fragColor;

float sdRR(vec2 p, vec2 c, vec2 hs, float r) {
    vec2 q = abs(p - c) - (hs - r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 p = gl_FragCoord.xy;
    vec2 c = Rect.xy + Rect.zw * 0.5, hs = Rect.zw * 0.5;
    float r = min(Radius, min(hs.x, hs.y));
    float d = sdRR(p, c, hs, r);
    float px = max(Mode.y, 1.0);

    if (Mode.z > 0.0) {   // sombra
        float a = Mode.z * (1.0 - smoothstep(-Mode.w, Mode.w, d));
        if (a <= 0.002) discard;
        fragColor = vec4(0.0, 0.0, 0.0, a) * ColorModulator;
        return;
    }
    if (d > 1.0) discard;
    float aa = clamp(0.5 - d, 0.0, 1.0);

    float e = 1.0;
    vec2 n = vec2(sdRR(p + vec2(e, 0.0), c, hs, r) - sdRR(p - vec2(e, 0.0), c, hs, r),
                  sdRR(p + vec2(0.0, e), c, hs, r) - sdRR(p - vec2(0.0, e), c, hs, r));
    n = n / max(length(n), 1e-5);

    bool recess = Mode.x > 0.5;
    float t = clamp(1.0 + d / max(Params.y, 1.0), 0.0, 1.0);
    float disp = pow(t, 2.2) * Params.x * (recess ? -1.0 : 1.0);
    vec3 col = vec3(texture(Backdrop, (p + n * disp * 1.05) / ScreenSize).r,
                    texture(Backdrop, (p + n * disp) / ScreenSize).g,
                    texture(Backdrop, (p + n * disp * 0.95) / ScreenSize).b);
    float l = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(l), col, 0.85);                 // um pouco menos saturado: o vidro "acalma" as cores de tras
    col = mix(col, Tint.rgb, Tint.a) + Params.w;

    vec2 L = normalize(vec2(-0.62, 0.78)) * (recess ? -1.0 : 1.0);   // luz vindo de cima/esquerda (y da tela cresce para cima aqui)
    float facing = clamp(dot(n, L), 0.0, 1.0);
    float dp = d / px;
    float edge = exp(-pow((dp + 0.6) / 0.55, 2.0));
    float inner = exp(-pow((dp + 2.5) / 2.0, 2.0));
    col += (edge * (0.16 + 0.70 * facing) + inner * 0.09 * facing) * Params.z;
    col -= edge * 0.10 * (1.0 - facing);
    fragColor = vec4(clamp(col, 0.0, 1.0), aa) * ColorModulator;
}
