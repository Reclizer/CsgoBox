#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;
uniform float Progress;

out vec4 fragColor;

void main() {
    // Keep the original radius and four-pass look, with a bounded sampling cost.
    // Previously Radius=50 required 101 samples per pass (404 per screen pixel).
    float radius = max(0.0, Radius * Progress);
    if (radius < 0.5) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }
    vec2 stepSize = oneTexel * BlurDir * (radius / 8.0);
    vec4 blurred = vec4(0.0);
    for (int tap = -8; tap <= 8; ++tap) {
        blurred += texture(DiffuseSampler, texCoord + float(tap) * stepSize);
    }
    // Normalize alpha as well; the previous shader accumulated it without averaging.
    fragColor = blurred / 17.0;
}
