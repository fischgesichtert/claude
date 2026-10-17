#version 150

// TIME – Zurückspulen: Look eines rückwärts laufenden Videobands.
// Farbsäume, Bildstand-Zittern, Rauschen, Laufstreifen und ein warmer, ausgebleichter Ton.

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
uniform float Intensity;

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    float t = Time;

    // Bildstand: ganze Zeilenblöcke ruckeln seitlich, ein Störstreifen läuft nach oben
    float band = floor(uv.y * 48.0);
    float jitter = (hash(vec2(band, floor(t * 14.0))) - 0.5) * 0.006;
    float stripePos = fract(-t * 0.35);
    float stripe = smoothstep(0.035, 0.0, abs(uv.y - stripePos));
    uv.x += (jitter + stripe * 0.02 * sin(uv.y * 400.0 + t * 50.0)) * Intensity;

    // Farbsäume (Rot/Blau versetzt)
    float shift = (1.5 + 2.5 * stripe) / InSize.x * Intensity * 3.0;
    vec3 color;
    color.r = texture(DiffuseSampler, uv + vec2(shift, 0.0)).r;
    color.g = texture(DiffuseSampler, uv).g;
    color.b = texture(DiffuseSampler, uv - vec2(shift, 0.0)).b;
    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    // Ausgebleicht, warm
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 tape = mix(color, vec3(lum) * vec3(1.08, 0.98, 0.82), 0.45);
    tape = tape * 0.92 + 0.04;

    // Zeilenraster und Rauschen
    float scan = 0.88 + 0.12 * sin(uv.y * InSize.y * 1.5);
    float grain = (hash(uv * InSize + t * 60.0) - 0.5) * 0.10;
    tape = tape * scan + grain + stripe * 0.25;

    // Rand abdunkeln
    float vignette = 1.0 - 0.35 * smoothstep(0.4, 0.95, length(texCoord - 0.5) * 1.4);
    tape *= vignette;

    fragColor = vec4(mix(original, clamp(tape, 0.0, 1.0), Intensity), 1.0);
}
