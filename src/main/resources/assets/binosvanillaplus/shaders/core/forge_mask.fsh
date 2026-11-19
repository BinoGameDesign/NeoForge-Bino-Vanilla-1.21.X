#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) discard;   // so o recorte da textura (silhueta)
    fragColor = vertexColor;
}
