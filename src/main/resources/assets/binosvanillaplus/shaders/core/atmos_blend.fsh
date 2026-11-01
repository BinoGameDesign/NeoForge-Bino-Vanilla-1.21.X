#version 150

// Aplica a neblina calculada pelo atmos_fog por cima da cena (cor ja multiplicada pela opacidade).
uniform sampler2D FogTex;

out vec4 fragColor;

void main() {
    fragColor = texelFetch(FogTex, ivec2(gl_FragCoord.xy), 0);
}
