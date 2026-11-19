#version 150

// Contorno da forja: pinta em volta da silhueta (mascara) uma borda solida de BORDER pixels e, depois dela,
// um brilho suave na cor do item que some em GLOW pixels.
// Borda: degrade do branco (lado de cima-direita) para a cor do item. Faixa (+4..+10) muda opacidade e brilho.
uniform sampler2D Sampler0;
uniform vec2 OutSize;
uniform float Scale;   // altura da tela / 1080

in vec2 uv;

out vec4 fragColor;

// ====== AJUSTES (pixels medidos em 1080p) ======
const float BORDER = 8.0;
const float GLOW = 18.0;
// por faixa (+4, +6, +8, +10)
const float ALPHA[5] = float[](0.0, 0.65, 0.80, 0.92, 1.0);       // borda
const float GLOW_ALPHA[5] = float[](0.0, 0.18, 0.26, 0.34, 0.45); // brilho
const int DIRS = 16;

void main() {
    if (texture(Sampler0, uv).a > 0.0) discard;   // dentro do item ou do corpo de quem segura: nada
    vec2 px = 1.0 / OutSize;
    float border = BORDER * Scale, glow = GLOW * Scale;
    int maxR = int(ceil(border + glow));
    float best = -1.0;
    vec4 bc = vec4(0.0);
    vec2 bd = vec2(0.0);
    for (int r = 1; r <= maxR && best < 0.0; r++) {
        for (int i = 0; i < DIRS; i++) {
            float ang = 6.2831853 * (float(i) + (r % 2 == 0 ? 0.5 : 0.0)) / float(DIRS);
            vec2 d = vec2(cos(ang), sin(ang)) * float(r);
            vec4 c = texture(Sampler0, uv + d * px);
            if (c.a > 0.1) { best = float(r); bc = c; bd = d; break; }   // so item (o corpo tem alfa ~0)
        }
    }
    if (best < 0.0) discard;
    int tier = clamp(int(bc.a * 4.0 + 0.5), 1, 4);
    if (best <= border) {
        vec2 outward = -normalize(bd);
        float s = (dot(outward, normalize(vec2(1.0, 1.0))) + 1.0) * 0.5;   // 1 = lado de cima-direita
        float w = 0.15 + 0.85 * s * s;
        fragColor = vec4(mix(bc.rgb, vec3(1.0), w), ALPHA[tier]);
    } else {
        float k = 1.0 - (best - border) / glow;
        fragColor = vec4(mix(bc.rgb, vec3(1.0), 0.25), GLOW_ALPHA[tier] * k * k);
    }
}
