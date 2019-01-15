attribute vec2 aVertexPosition;

uniform vec2 model;
uniform float scale;
uniform mat4 view;
uniform mat4 projection;

varying vec2 vTexCoord;

void main() {
    gl_Position = projection * view * vec4(scale * (aVertexPosition + 1.0) + model, 0.0, 1.0);
    vTexCoord = 0.5 * aVertexPosition + 0.5;
}