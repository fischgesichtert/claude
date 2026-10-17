package com.fischgesichtert.timemod.era;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Era.Age;
import com.fischgesichtert.timemod.era.Era.Mood;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Die feste Stufenliste der Zeitmaschine, vom Trias bis +1000 Jahre. Die Dimensionen dazu erzeugt
 * {@code tools/gen_eras.py}; beide Listen müssen zusammenpassen (das prüft ein Gametest).
 */
public final class Eras {
    private static final String PREFIX = "year_";
    private static final List<Era> ALL = new ArrayList<>();
    private static final Map<ResourceKey<Level>, Era> BY_LEVEL = new HashMap<>();
    private static final Map<Integer, Era> BY_YEARS = new HashMap<>();

    public static final Era PRESENT;

    static {
        // Tiefe Vergangenheit: eigene Kontinente, Meeresspiegel und Urzeit-Biome
        add(-245_000_000, Age.MESOZOIC, new Mood(0xF2A26E, 0.30F, 0.60F));
        add(-200_000_000, Age.MESOZOIC, new Mood(0xE8946A, 0.28F, 0.55F));
        add(-150_000_000, Age.MESOZOIC, new Mood(0xB9D7A0, 0.25F, 0.55F));
        add(-100_000_000, Age.MESOZOIC, new Mood(0xE6C88E, 0.20F, 0.65F));
        add(-70_000_000, Age.MESOZOIC, new Mood(0xF0C080, 0.22F, 0.65F));
        add(-66_000_000, Age.MESOZOIC, new Mood(0xF0A868, 0.30F, 0.55F));
        add(-10_000_000, Age.CENOZOIC, new Mood(0xF2E0B4, 0.15F, 0.70F));
        add(-1_000_000, Age.CENOZOIC, new Mood(0xE8E8E0, 0.12F, 0.70F));
        // Eiszeit: dieselben Kontinente, weniger Meer, Gletscher und Schneedunst
        add(-100_000, Age.ICE_AGE, new Mood(0xDCE6F4, 0.25F, 0.50F));
        add(-20_000, Age.ICE_AGE, new Mood(0xE6EEFA, 0.40F, 0.35F));
        add(-5_000, Age.STONE_AGE, Mood.CLEAR);
        for (int years : new int[] {-1000, -500, -100, -50, -10, -5, -4, -3, -2, -1}) {
            add(years, Age.HISTORY, Mood.CLEAR);
        }
        PRESENT = add(0, Age.PRESENT, Mood.CLEAR);
        for (int years : new int[] {1, 2, 3, 4, 5, 10, 25, 50, 100, 250, 500, 1000}) {
            add(years, Age.FUTURE, Mood.CLEAR);
        }
    }

    private Eras() {
    }

    private static Era add(int years, Age age, Mood mood) {
        ResourceKey<Level> level = years == 0 ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, TimeMod.id(PREFIX + Era.idOf(years)));
        Era era = new Era(years, age, level, mood);
        ALL.add(era);
        BY_LEVEL.put(level, era);
        BY_YEARS.put(years, era);
        return era;
    }

    /** Alle Epochen samt Gegenwart, vom ältesten zum jüngsten Jahr. */
    public static List<Era> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Die Epoche einer Dimension; die Oberwelt ist die Gegenwart, Nether und End sind keine Epoche. */
    @Nullable
    public static Era byLevel(ResourceKey<Level> level) {
        return BY_LEVEL.get(level);
    }

    @Nullable
    public static Era byYears(int years) {
        return BY_YEARS.get(years);
    }

    /** Ob die Dimension eine der Epochen-Welten ist (nicht die Gegenwart). */
    public static boolean isEraDimension(ResourceLocation id) {
        return id.getNamespace().equals(TimeMod.MOD_ID) && id.getPath().startsWith(PREFIX);
    }

    public static int indexOf(Era era) {
        return ALL.indexOf(era);
    }

    /** Eine Stufe vor (+1) oder zurück (−1) in der Liste; an den Enden bleibt es stehen. */
    public static Era step(Era era, int direction) {
        int index = Math.max(0, Math.min(ALL.size() - 1, indexOf(era) + Integer.signum(direction)));
        return ALL.get(index);
    }

    /** Schnellsprung: so lange weiter, bis ein neues Zeitalter beginnt – oder die Liste endet. */
    public static Era jump(Era era, int direction) {
        Era current = era;
        while (true) {
            Era next = step(current, direction);
            if (next == current) {
                return current;
            }
            if (next.age() != era.age()) {
                return next;
            }
            current = next;
        }
    }
}
