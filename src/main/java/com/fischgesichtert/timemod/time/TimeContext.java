package com.fischgesichtert.timemod.time;

import net.minecraft.world.level.Level;

/**
 * Eigenzeit-Kontext: Während ein Objekt in einer Zeitzone tickt, sieht es über
 * {@code Level#getGameTime()} seine eigene Zeit statt der Weltzeit.
 * <p>
 * Ohne das würden z. B. KI-Verhaltensweisen, deren Dauer über Zeitstempel gemessen wird,
 * bei 2× doppelt so viele Ticks laufen und das Ergebnis wäre kein echtes „schnelleres Video“.
 */
public final class TimeContext {
    private static Level level;
    private static long offset;

    private TimeContext() {
    }

    public static void enter(Level l, long eigenzeitOffset) {
        level = l;
        offset = eigenzeitOffset;
    }

    public static void exit() {
        level = null;
        offset = 0;
    }

    public static long adjust(Level l, long gameTime) {
        return l == level ? gameTime + offset : gameTime;
    }

    public static boolean isActive() {
        return level != null;
    }
}
