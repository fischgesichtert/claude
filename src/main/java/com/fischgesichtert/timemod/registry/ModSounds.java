package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Alle Klänge sind selbst synthetisiert (siehe tools/gen_sounds.py). */
public final class ModSounds {
    public static final SoundEvent TIME_STOP = register("time_stop");
    public static final SoundEvent TIME_RESUME = register("time_resume");
    public static final SoundEvent CLOCK_TICK = register("clock_tick");
    public static final SoundEvent CLOCK_WIND = register("clock_wind");
    public static final SoundEvent HEARTBEAT = register("heartbeat");
    public static final SoundEvent STOP_AMBIENCE = register("stop_ambience");
    public static final SoundEvent FAST_FORWARD = register("fast_forward");
    public static final SoundEvent SLOW_MOTION = register("slow_motion");
    public static final SoundEvent REWIND = register("rewind");
    public static final SoundEvent ZONE_HUM = register("zone_hum");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, TimeMod.id(name), SoundEvent.createVariableRangeEvent(TimeMod.id(name)));
    }

    public static void init() {
        // Laden der Klasse registriert die Klänge.
    }
}
