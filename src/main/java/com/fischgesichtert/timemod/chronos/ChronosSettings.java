package com.fischgesichtert.timemod.chronos;

import com.fischgesichtert.timemod.time.TimeFilter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Einstellungen der Chronos-Uhr (im Item gespeichert) und ihre Grenzen je Meisterschaftsstufe.
 * Alles wird beim Lesen auf die erlaubten Werte begrenzt – der Client kann nichts erzwingen.
 */
public final class ChronosSettings {
    public enum Mode {
        STOP, FAST, SLOW, REWIND
    }

    /** Was das Radialmenü und das Mausrad verstellen dürfen (Paket an den Server). */
    public enum Action {
        /** Wert = Modus. */
        MODE,
        /** Wert = Index in {@link #FILTERS}. */
        FILTER,
        RESET_FILTER,
        GLOBAL,
        SELF,
        /** Hauptwert des Modus um ±Wert: Stopp-Dauer, Faktor oder Rückspul-Tiefe. */
        VALUE,
        /** Dauer von Zeitraffer und Zeitlupe um ±Wert. */
        DURATION,
        RADIUS
    }

    public static final String KEY = "Chronos";
    public static final double[] FAST_FACTORS = {1.25, 1.5, 2, 3, 4, 5, 6, 8, 10};
    public static final double[] SLOW_FACTORS = {0.75, 0.5, 0.25, 0.15, 0.1, 0.05};
    /** Dauer von Zeitraffer- und Zeitlupenzonen in Sekunden. */
    public static final int[] DURATIONS = {5, 10, 20, 30, 60, 120};
    public static final int[] MASTERY_THRESHOLDS = {0, 25, 75, 175, 350, 700};
    public static final int RADIUS_STEP = 4;
    public static final int MIN_RADIUS = 4;

    /** Die Filter der Uhr: alles, was es als Filtermodul gibt, dazu Tageszeit und Wetter. */
    public static final int[] FILTERS = {TimeFilter.ANIMALS, TimeFilter.MONSTERS, TimeFilter.VILLAGERS, TimeFilter.PLAYERS,
        TimeFilter.PROJECTILES | TimeFilter.OTHER_ENTITIES, TimeFilter.ITEMS, TimeFilter.VEHICLES, TimeFilter.GROWTH,
        TimeFilter.ENVIRONMENT, TimeFilter.FLUIDS, TimeFilter.BLOCK_TICKS, TimeFilter.MACHINES, TimeFilter.DAYTIME,
        TimeFilter.WEATHER | TimeFilter.WORLD_EVENTS};
    public static final String[] FILTER_NAMES = {"animals", "monsters", "villagers", "players", "projectiles", "items", "vehicles",
        "growth", "environment", "fluids", "redstone", "machines", "daytime", "weather"};
    /** Wirken nur global (Tag/Nacht, Wetter). */
    public static final int GLOBAL_ONLY = TimeFilter.DAYTIME | TimeFilter.WEATHER | TimeFilter.WORLD_EVENTS;
    private static final int ALLOWED = TimeFilter.ALL_ENTITIES | TimeFilter.ALL_BLOCKS | TimeFilter.ALL_WORLD;

    public Mode mode = Mode.STOP;
    public int fastIndex = 2;
    public int slowIndex = 1;
    public int radius = 12;
    public int stopSeconds = 3;
    public int durationIndex = 2;
    public int rewindSeconds = 10;
    /** Filterkategorien; 0 = Standard des jeweiligen Modus. */
    public int mask;
    public boolean global;
    /** Beim Zurückspulen selbst mitspulen (sonst bleibt man stehen und kann jederzeit loslassen). */
    public boolean self;
    public int mastery;

    public static ChronosSettings of(ItemStack stack) {
        ChronosSettings s = new ChronosSettings();
        CompoundTag tag = stack.getTagElement(KEY);
        if (tag != null) {
            s.mode = Mode.values()[Mth.clamp(tag.getInt("Mode"), 0, Mode.values().length - 1)];
            s.fastIndex = tag.contains("Fast") ? tag.getInt("Fast") : s.fastIndex;
            s.slowIndex = tag.contains("Slow") ? tag.getInt("Slow") : s.slowIndex;
            s.radius = tag.contains("Radius") ? tag.getInt("Radius") : s.radius;
            s.stopSeconds = tag.contains("Stop") ? tag.getInt("Stop") : s.stopSeconds;
            s.durationIndex = tag.contains("Duration") ? tag.getInt("Duration") : s.durationIndex;
            s.rewindSeconds = tag.contains("Rewind") ? tag.getInt("Rewind") : s.rewindSeconds;
            s.mask = tag.getInt("Mask") & ALLOWED;
            s.global = tag.getBoolean("Global");
            s.self = tag.getBoolean("Self");
            s.mastery = Math.max(0, tag.getInt("Mastery"));
        }
        s.clamp();
        return s;
    }

    public void save(ItemStack stack) {
        clamp();
        CompoundTag tag = stack.getOrCreateTagElement(KEY);
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("Fast", fastIndex);
        tag.putInt("Slow", slowIndex);
        tag.putInt("Radius", radius);
        tag.putInt("Stop", stopSeconds);
        tag.putInt("Duration", durationIndex);
        tag.putInt("Rewind", rewindSeconds);
        tag.putInt("Mask", mask);
        tag.putBoolean("Global", global);
        tag.putBoolean("Self", self);
        tag.putInt("Mastery", mastery);
    }

    /** Alle Werte in die Grenzen der aktuellen Meisterschaft bringen. */
    public void clamp() {
        int level = level();
        if (global && !globalUnlocked(level)) {
            global = false;
        }
        fastIndex = Mth.clamp(fastIndex, 0, maxFastIndex(level, global));
        slowIndex = Mth.clamp(slowIndex, 0, maxSlowIndex(level));
        radius = Mth.clamp(radius, MIN_RADIUS, maxRadius(level));
        stopSeconds = Mth.clamp(stopSeconds, 1, maxStopSeconds(level));
        durationIndex = Mth.clamp(durationIndex, 0, DURATIONS.length - 1);
        rewindSeconds = Mth.clamp(rewindSeconds, 2, maxRewindSeconds(level));
    }

    // ------------------------------------------------------------------ Aktionen (vom Client)

    /** Führt eine Aktion aus dem Radialmenü oder vom Mausrad aus. Liefert, ob sich etwas geändert hat. */
    public boolean apply(Action action, int value) {
        int delta = Mth.clamp(value, -10, 10);
        ChronosSettings before = copy();
        switch (action) {
            case MODE -> mode = Mode.values()[Mth.clamp(value, 0, Mode.values().length - 1)];
            case FILTER -> {
                if (value < 0 || value >= FILTERS.length) {
                    return false;
                }
                // Mindestens ein Filter bleibt an (0 hieße „Standard“)
                int next = effectiveMask() ^ FILTERS[value];
                if (next == 0) {
                    return false;
                }
                mask = next;
            }
            case RESET_FILTER -> mask = 0;
            case GLOBAL -> global = !global && globalUnlocked(level());
            case SELF -> self = !self;
            case VALUE -> adjust(delta);
            case DURATION -> durationIndex += delta;
            case RADIUS -> radius += delta * RADIUS_STEP;
        }
        clamp();
        return !equals(before);
    }

    /** Hauptwert des Modus verstellen (Mausrad): Stopp-Dauer, Faktor oder Rückspul-Tiefe. */
    public void adjust(int delta) {
        switch (mode) {
            case STOP -> stopSeconds += delta;
            case FAST -> fastIndex += delta;
            case SLOW -> slowIndex += delta;
            case REWIND -> {
                // Feine Schritte bis 10 s, darüber in 5-Sekunden-Schritten
                for (int i = 0; i < Math.abs(delta); i++) {
                    boolean up = delta > 0;
                    int step = up ? (rewindSeconds < 10 ? 1 : 5) : (rewindSeconds <= 10 ? 1 : 5);
                    rewindSeconds += up ? step : -step;
                }
            }
        }
        clamp();
    }

    public int effectiveMask() {
        return mask != 0 ? mask : defaultMask(mode);
    }

    /** Ohne eigene Filter: Stopp und Zurückspulen erfassen auch andere Spieler, Raffer und Lupe nicht. */
    public static int defaultMask(Mode mode) {
        return mode == Mode.STOP || mode == Mode.REWIND ? TimeFilter.DEFAULT | TimeFilter.PLAYERS : TimeFilter.DEFAULT;
    }

    public ChronosSettings copy() {
        ChronosSettings s = new ChronosSettings();
        s.mode = mode;
        s.fastIndex = fastIndex;
        s.slowIndex = slowIndex;
        s.radius = radius;
        s.stopSeconds = stopSeconds;
        s.durationIndex = durationIndex;
        s.rewindSeconds = rewindSeconds;
        s.mask = mask;
        s.global = global;
        s.self = self;
        s.mastery = mastery;
        return s;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ChronosSettings s && s.mode == mode && s.fastIndex == fastIndex && s.slowIndex == slowIndex
            && s.radius == radius && s.stopSeconds == stopSeconds && s.durationIndex == durationIndex && s.rewindSeconds == rewindSeconds
            && s.mask == mask && s.global == global && s.self == self && s.mastery == mastery;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(mode, fastIndex, slowIndex, radius, stopSeconds, durationIndex, rewindSeconds, mask, global, self, mastery);
    }

    // ------------------------------------------------------------------ Meisterschaft

    public int level() {
        int level = 0;
        for (int i = 0; i < MASTERY_THRESHOLDS.length; i++) {
            if (mastery >= MASTERY_THRESHOLDS[i]) {
                level = i;
            }
        }
        return level;
    }

    public static int maxLevel() {
        return MASTERY_THRESHOLDS.length - 1;
    }

    /** Fortschritt zur nächsten Stufe (0..1); 1 auf der höchsten. */
    public float levelProgress() {
        int level = level();
        if (level >= maxLevel()) {
            return 1.0F;
        }
        int from = MASTERY_THRESHOLDS[level];
        int to = MASTERY_THRESHOLDS[level + 1];
        return (mastery - from) / (float) (to - from);
    }

    public static int maxStopSeconds(int level) {
        return new int[] {3, 4, 5, 6, 8, 10}[level];
    }

    public static int maxRadius(int level) {
        return new int[] {16, 24, 32, 40, 48, 64}[level];
    }

    public static int maxRewindSeconds(int level) {
        return new int[] {20, 30, 40, 50, 60, 60}[level];
    }

    public static boolean globalUnlocked(int level) {
        return level >= 3;
    }

    /** Lokal bis 6×–10×, global erst ab Stufe 3 und höchstens 2×/3×/5×. */
    public static int maxFastIndex(int level, boolean global) {
        if (global) {
            double max = level >= 5 ? 5 : level >= 4 ? 3 : 2;
            return indexAtMost(FAST_FACTORS, max);
        }
        double max = new double[] {6, 6, 8, 10, 10, 10}[level];
        return indexAtMost(FAST_FACTORS, max);
    }

    public static int maxSlowIndex(int level) {
        double min = new double[] {0.25, 0.15, 0.1, 0.05, 0.05, 0.05}[level];
        int index = 0;
        for (int i = 0; i < SLOW_FACTORS.length; i++) {
            if (SLOW_FACTORS[i] >= min - 1e-9) {
                index = i;
            }
        }
        return index;
    }

    private static int indexAtMost(double[] values, double max) {
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= max + 1e-9) {
                index = i;
            }
        }
        return index;
    }

    /** Effizienz: je Stufe 6 % billiger. */
    public double efficiency() {
        return 1.0 - 0.06 * level();
    }

    // ------------------------------------------------------------------ Werte des aktuellen Modus

    public double factor() {
        return switch (mode) {
            case STOP, REWIND -> 0.0;
            case FAST -> FAST_FACTORS[fastIndex];
            case SLOW -> SLOW_FACTORS[slowIndex];
        };
    }

    /** Wirkdauer in Sekunden (Stopp: eigene Dauer; Raffer und Lupe: Zonendauer; Zurückspulen: Tiefe). */
    public int seconds() {
        return switch (mode) {
            case STOP -> stopSeconds;
            case FAST, SLOW -> DURATIONS[durationIndex];
            case REWIND -> rewindSeconds;
        };
    }
}
