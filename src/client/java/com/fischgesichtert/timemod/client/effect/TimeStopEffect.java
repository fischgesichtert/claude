package com.fischgesichtert.timemod.client.effect;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.ClientClock;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Bildeffekt des Zeitstopps: Eine invertierte Kugelwelle rast vom Zentrum nach außen und lässt
 * alles, was sie überstreicht, ergrauen. Beim Weiterlaufen der Zeit zieht sie sich zusammen.
 * Die Welle läuft in Spielzeit, damit sie auch in langsamen Aufnahmen sichtbar ist.
 */
public final class TimeStopEffect {
    private static final PostEffect SHADER = new PostEffect(TimeMod.id("shaders/post/time_stop.json"));
    /** Ausbreitungsgeschwindigkeit der Welle in Blöcken pro Sekunde (Spielzeit). */
    private static final double WAVE_SPEED = 28.0;
    private static final double RESUME_SPEED = 45.0;

    private static final Map<Integer, Wave> WAVES = new HashMap<>();

    private TimeStopEffect() {
    }

    private static final class Wave {
        final Vec3 center;
        final double radius;
        final boolean global;
        final double start = ClientClock.seconds();
        double resumeStart = -1;

        Wave(Vec3 center, double radius, boolean global) {
            this.center = center;
            this.radius = radius;
            this.global = global;
        }

        double front() {
            double now = ClientClock.seconds();
            if (resumeStart >= 0) {
                return Math.max(0.0, radius - (now - resumeStart) * RESUME_SPEED);
            }
            return Math.min(radius + 4.0, (now - start) * WAVE_SPEED);
        }

        boolean finished() {
            return resumeStart >= 0 && front() <= 0.0;
        }
    }

    /** Wird aufgerufen, wenn sich die Zonenliste ändert. */
    public static void onZonesChanged(List<TimeZone> before, List<TimeZone> after) {
        for (TimeZone zone : after) {
            if (zone.isStop() && !zone.isRewind() && !WAVES.containsKey(zone.id())) {
                Center c = center(zone.shape());
                WAVES.put(zone.id(), new Wave(c.pos, c.radius, zone.shape().isGlobal()));
            }
        }
        List<Integer> ended = new ArrayList<>();
        for (var entry : WAVES.entrySet()) {
            boolean stillThere = after.stream().anyMatch(z -> z.id() == entry.getKey() && z.isStop());
            if (!stillThere && entry.getValue().resumeStart < 0) {
                ended.add(entry.getKey());
            }
        }
        for (int id : ended) {
            WAVES.get(id).resumeStart = ClientClock.seconds();
        }
        RewindEffect.onZonesChanged(after);
    }

    private record Center(Vec3 pos, double radius) {
    }

    private static Center center(ZoneShape shape) {
        if (shape instanceof ZoneShape.Sphere sphere) {
            return new Center(sphere.center(), sphere.radius());
        }
        AABB box = shape.bounds();
        if (box != null) {
            Vec3 c = box.getCenter();
            double r = 0.5 * Math.sqrt(box.getXsize() * box.getXsize() + box.getYsize() * box.getYsize() + box.getZsize() * box.getZsize());
            return new Center(c, r);
        }
        // Globale Zone: Welle geht vom Spieler aus und erfasst die ganze Sichtweite
        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = mc.player != null ? mc.player.getEyePosition() : Vec3.ZERO;
        return new Center(pos, 512.0);
    }

    public static boolean isActive() {
        return !WAVES.isEmpty();
    }

    /** Wie stark der Stillstand gerade an der Kamera wirkt (0..1) – für Ton und Vignette. */
    public static float cameraStillness() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || WAVES.isEmpty()) {
            return 0.0F;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (Wave wave : WAVES.values()) {
            double d = cam.distanceTo(wave.center);
            if (d <= wave.radius && wave.front() >= d) {
                return 1.0F;
            }
        }
        return 0.0F;
    }

    public static void clear() {
        WAVES.clear();
        RewindEffect.clear();
    }

    public static void reset() {
        SHADER.reset();
        RewindEffect.reset();
    }

    /** Am Ende des Welt-Renderns: Effekte über das Bild legen (die Hand bleibt farbig). */
    public static void render(WorldRenderContext context) {
        RewindEffect.render(context);
        WAVES.values().removeIf(Wave::finished);
        if (WAVES.isEmpty()) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        Wave wave = nearest(cam);
        double front = wave.front();
        double camDist = cam.distanceTo(wave.center);
        boolean cameraInside = camDist <= wave.radius;
        float gray = wave.resumeStart >= 0 ? (float) Mth.clamp(front / Math.max(wave.radius, 1.0), 0.0, 1.0) : 1.0F;
        float flash = (float) Math.max(0.0, 1.0 - Math.abs(camDist - front) / 2.5) * (cameraInside ? 0.85F : 0.0F);

        Matrix4f viewProj = new Matrix4f(context.projectionMatrix()).mul(context.matrixStack().last().pose());
        Matrix4f inverse = viewProj.invert(new Matrix4f());
        Vec3 rel = wave.center.subtract(cam);
        float time = (float) (ClientClock.seconds() - wave.start);

        SHADER.process(context.tickDelta(), effect -> {
            effect.safeGetUniform("InvViewProj").set(inverse);
            effect.safeGetUniform("ZoneCenter").set((float) rel.x, (float) rel.y, (float) rel.z);
            effect.safeGetUniform("ZoneRadius").set((float) wave.radius);
            effect.safeGetUniform("WaveRadius").set((float) front);
            effect.safeGetUniform("WaveWidth").set(wave.global ? 6.0F : 1.6F);
            effect.safeGetUniform("GrayAmount").set(gray);
            effect.safeGetUniform("CameraInside").set(cameraInside ? 1.0F : 0.0F);
            effect.safeGetUniform("Flash").set(flash);
            effect.safeGetUniform("Time").set(time);
        });
    }

    private static Wave nearest(Vec3 cam) {
        Wave best = null;
        double bestScore = Double.MAX_VALUE;
        for (Wave wave : WAVES.values()) {
            double score = Math.max(0.0, cam.distanceTo(wave.center) - wave.radius);
            if (score < bestScore) {
                bestScore = score;
                best = wave;
            }
        }
        return best;
    }
}
