precision mediump float;

uniform sampler2D uTexture;

varying vec2 vTexCoord;
varying float vMix;

void main() {
    vec4 col = texture2D(uTexture, vTexCoord);
    if (vMix > 0.0 && col.w < 0.6) {
        col = vec4(0.6, 0.6, 0.6, 0.8);
    }
    gl_FragColor = col;
}