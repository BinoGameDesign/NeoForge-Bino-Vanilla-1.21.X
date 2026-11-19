#version 150

// Contorno da forja: pinta os pixels de FORA da silhueta (mascara) que estao a ate N pixels dela.
// Cor: degrade do branco (lado de cima-direita da silhueta) para a cor do item (outros lados).
uniform sampler2D Sampler0;
uniform vec2 OutSize;
uniform float Scale;   // altura da tela / 1080

in vec2 uv;

out vec4 fragColor;

// por faixa (+4, +6, +8, +10): espessura em pixels (a 1080p) e opacidade
const float WIDTH[5] = float[](0.0, 2.0, 2.5, 3.0, 3.5);
const float ALPHA[5] = float[](0.0, 0.60, 0.75, 0.90, 1.0);

void main() {
    if (texture(Sampler0, uv).a > 0.0) discard;   // dentro do item: nada
    vec2 px = 1.0 / OutSize;
    int R = int(ceil(WIDTH[4] * Scale));
    float best = 1e9;
    vec4 bc = vec4(0.0);
    vec2 bd = vec2(0.0);
    for (int y = -R; y <= R; y++) {
        for (int x = -R; x <= R; x++) {
            float d = length(vec2(float(x), float(y)));
            if (d < 0.5 || d > float(R) + 0.01 || d >= best) continue;
            vec4 c = texture(Sampler0, uv + vec2(float(x), float(y)) * px);
            if (c.a <= 0.0) continue;
            int t = clamp(int(c.a * 4.0 + 0.5), 1, 4);
            if (d > WIDTH[t] * Scale + 0.01) continue;
            best = d; bc = c; bd = vec2(float(x), float(y));
        }
    }
    if (best > 1e8) discard;
    int tier = clamp(int(bc.a * 4.0 + 0.5), 1, 4);
    vec2 outward = -normalize(bd);
    float s = (dot(outward, normalize(vec2(1.0, 1.0))) + 1.0) * 0.5;   // 1 = lado de cima-direita
    float w = 0.12 + 0.88 * s * s;
    fragColor = vec4(mix(bc.rgb, vec3(1.0), w), ALPHA[tier]);
}
