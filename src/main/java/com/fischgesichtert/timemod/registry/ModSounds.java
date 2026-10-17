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
    public static final SoundEvent CLOCKMAKER_ROAR = register("clockmaker_roar");
    public static final SoundEvent GEAR_SAW = register("gear_saw");
    public static final SoundEvent PHAGE_ROAR = register("phage_roar");
    public static final SoundEvent PHAGE_OPEN = register("phage_open");
    public static final SoundEvent PHAGE_BITE = register("phage_bite");
    public static final SoundEvent PHAGE_DRAIN = register("phage_drain");
    public static final SoundEvent BOSS_AWAKEN = register("boss_awaken");
    public static final SoundEvent PARADOX_VOICE = register("paradox_voice");
    public static final SoundEvent PARADOX_BLINK = register("paradox_blink");
    public static final SoundEvent ECHO_SHATTER = register("echo_shatter");
    public static final SoundEvent GATE_CHARGE = register("gate_charge");
    public static final SoundEvent GATE_OPEN = register("gate_open");
    public static final SoundEvent GATE_CLOSE = register("gate_close");
    public static final SoundEvent GATE_ARRIVE = register("gate_arrive");
    public static final SoundEvent CONSOLE_CLICK = register("console_click");
    public static final SoundEvent TIME_STREAM = register("time_stream");
    public static final SoundEvent PARADOX_WARNING = register("paradox_warning");
    public static final SoundEvent WARDEN_APPEAR = register("warden_appear");
    public static final SoundEvent WARDEN_VANISH = register("warden_vanish");
    public static final SoundEvent WARDEN_BLINK = register("warden_blink");
    public static final SoundEvent WARDEN_AMBIENT = register("warden_ambient");
    public static final SoundEvent WARDEN_HURT = register("warden_hurt");
    public static final SoundEvent WARDEN_DEATH = register("warden_death");
    public static final SoundEvent WARDEN_ARREST = register("warden_arrest");
    public static final SoundEvent WARDEN_BOLT = register("warden_bolt");
    public static final SoundEvent WARDEN_BOLT_HIT = register("warden_bolt_hit");
    public static final SoundEvent SEAL_STAMP = register("seal_stamp");
    public static final SoundEvent GUARDIAN_WAKE = register("guardian_wake");
    public static final SoundEvent COLOSSUS_EMERGE = register("colossus_emerge");
    public static final SoundEvent COLOSSUS_BELL = register("colossus_bell");
    public static final SoundEvent COLOSSUS_BREATH = register("colossus_breath");
    public static final SoundEvent COLOSSUS_STOMP = register("colossus_stomp");
    public static final SoundEvent COLOSSUS_HURT = register("colossus_hurt");
    public static final SoundEvent COLOSSUS_DEATH = register("colossus_death");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, TimeMod.id(name), SoundEvent.createVariableRangeEvent(TimeMod.id(name)));
    }

    public static void init() {
        // Laden der Klasse registriert die Klänge.
    }
}
