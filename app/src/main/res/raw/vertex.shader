attribute vec2 aVertexPosition;

uniform vec2 model;
uniform mat4 view;
uniform mat4 projection;

varying vec2 vTexCoord;

void main() {
    gl_Position = projection * view * vec4(0.5 * aVertexPosition + model + 0.5, 0.0, 1.0);
    vTexCoord = 0.5 * aVertexPosition + 0.5;
}