#version 150

// TIME – Zeitstopp: Eine invertierte Kugelwelle breitet sich vom Zentrum aus,
// alles, was sie überstrichen hat, verliert seine Farbe.

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;
uniform vec3 ZoneCenter;     // relativ zur Kamera
uniform float ZoneRadius;
uniform float WaveRadius;
uniform float WaveWidth;
uniform float GrayAmount;
uniform float CameraInside;
uniform float Flash;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

vec3 viewPosition(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 pos = InvViewProj * ndc;
    return pos.xyz / pos.w;
}

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;
    float sky = step(0.999999, depth);
    vec3 pos = viewPosition(texCoord, depth);
    float dist = length(pos - ZoneCenter);

    // Bereich, den die Welle schon erfasst hat (nie größer als die Zone selbst)
    float covered = min(WaveRadius, ZoneRadius);
    float inside = 1.0 - smoothstep(covered - 0.6, covered + 0.6, dist);
    // Himmel: grau, wenn die Kamera in der Zone steht und die Welle den Rand erreicht hat
    float skyInside = CameraInside * smoothstep(ZoneRadius * 0.8, ZoneRadius, WaveRadius);
    inside = mix(inside, skyInside, sky);

    // Farbverlust mit kühlem Stich und etwas mehr Kontrast
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(lum) * vec3(0.90, 0.94, 1.10);
    gray = clamp((gray - 0.5) * 1.18 + 0.5, 0.0, 1.0);
    vec3 result = mix(color, gray, inside * GrayAmount);

    // Wellenfront: invertierte, leicht violette Schale
    float shell = 1.0 - smoothstep(0.0, WaveWidth, abs(dist - WaveRadius));
    shell *= (1.0 - sky) * (1.0 - step(ZoneRadius + WaveWidth, WaveRadius));
    vec3 inverted = (vec3(1.0) - color) * vec3(0.85, 0.72, 1.18);
    result = mix(result, inverted, shell * 0.95);

    // Kurzer Vollbild-Negativblitz, wenn die Welle über die Kamera hinwegzieht
    result = mix(result, (vec3(1.0) - result) * vec3(0.9, 0.8, 1.15), Flash);

    // Vignette, solange man selbst im Stillstand steht
    float edge = smoothstep(0.35, 0.95, length(texCoord - 0.5) * 1.45);
    result *= 1.0 - CameraInside * GrayAmount * 0.5 * edge;

    fragColor = vec4(result, 1.0);
}
