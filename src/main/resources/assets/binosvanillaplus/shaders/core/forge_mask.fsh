#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // item: so o recorte da textura (silhueta). Corpo de quem segura (uv < 0): sem textura, so bloqueia
    if (texCoord0.x >= 0.0 && texture(Sampler0, texCoord0).a < 0.1) discard;
    fragColor = vertexColor;
}
