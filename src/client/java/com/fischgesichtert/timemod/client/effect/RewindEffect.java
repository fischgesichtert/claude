package com.fischgesichtert.timemod.client.effect;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.ClientClock;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Bildeffekt beim Zurückspulen: rückwärts laufendes Videoband, solange die Kamera in der Zone ist. */
public final class RewindEffect {
    private static final PostEffect SHADER = new PostEffect(TimeMod.id("shaders/post/rewind.json"));
    private static final List<TimeZone> REWINDS = new ArrayList<>();
    private static float intensity;
    private static double lastSeconds;

    private RewindEffect() {
    }

    static void onZonesChanged(List<TimeZone> zones) {
        REWINDS.clear();
        for (TimeZone zone : zones) {
            if (zone.isRewind()) {
                REWINDS.add(zone);
            }
        }
    }

    /** Steht die Kamera gerade in einer Rückspul-Zone? */
    public static boolean cameraInRewind() {
        Minecraft mc = Minecraft.getInstance();
        if (REWINDS.isEmpty() || mc.gameRenderer == null) {
            return false;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (TimeZone zone : REWINDS) {
            if (zone.shape().contains(cam.x, cam.y, cam.z)) {
                return true;
            }
        }
        return false;
    }

    static void clear() {
        REWINDS.clear();
        intensity = 0.0F;
    }

    static void reset() {
        SHADER.reset();
    }

    static void render(WorldRenderContext context) {
        double now = ClientClock.seconds();
        float dt = (float) Mth.clamp(now - lastSeconds, 0.0, 0.25);
        lastSeconds = now;
        float target = cameraInRewind() ? 1.0F : 0.0F;
        intensity += (target - intensity) * Math.min(1.0F, dt * 6.0F);
        if (intensity < 0.01F) {
            return;
        }
        float strength = intensity;
        SHADER.process(context.tickDelta(), effect -> {
            effect.safeGetUniform("Time").set((float) now);
            effect.safeGetUniform("Intensity").set(strength);
        });
    }
}
