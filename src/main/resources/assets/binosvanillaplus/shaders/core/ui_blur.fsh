#version 150
// Desfoque gaussiano de 9 amostras numa direcao (Dir = passo em UV). Duas passadas (horizontal + vertical) = desfoque redondo.
uniform sampler2D Src;
uniform vec2 Dir;
in vec2 uv;
out vec4 fragColor;
void main() {
    vec3 c = texture(Src, uv).rgb * 0.2270270;
    c += (texture(Src, uv + Dir * 1.3846154).rgb + texture(Src, uv - Dir * 1.3846154).rgb) * 0.3162162;
    c += (texture(Src, uv + Dir * 3.2307692).rgb + texture(Src, uv - Dir * 3.2307692).rgb) * 0.0702703;
    fragColor = vec4(c, 1.0);
}
