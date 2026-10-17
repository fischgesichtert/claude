package com.fischgesichtert.timemod.safety;

import com.fischgesichtert.timemod.TimeMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Absturzschutz: TIME darf das Spiel niemals zum Absturz bringen.
 * <p>
 * Ein Fehler in der Mod wird abgefangen und beim ersten Mal mit vollständigem Stacktrace ins Log
 * geschrieben, danach höchstens einmal pro Minute zusammengefasst. Das Spiel macht an dieser Stelle
 * weiter, als gäbe es die Mod dort nicht. Fehler anderer Mods bleiben unangetastet, außer TIME
 * steckt selbst im Aufruf, der gescheitert ist ({@link #blames}).
 */
public final class Safety {
    private static final String PACKAGE = "com.fischgesichtert.timemod.";
    private static final long SUMMARY_MS = 60_000L;
    /** Längere Stacktraces schneidet die JVM ab; dann fehlen die äußeren Aufrufe. */
    private static final int MAX_TRACE = 1024;
    private static final Map<String, Site> SITES = new ConcurrentHashMap<>();

    private Safety() {
    }

    private static final class Site {
        private long count;
        private long logged;
        private long lastLog;
    }

    /** Meldet einen abgefangenen Fehler. Nur echte JVM-Notfälle (Speicher voll) werden weitergeworfen. */
    public static void report(String where, Throwable error) {
        if (error instanceof VirtualMachineError fatal && !(error instanceof StackOverflowError)) {
            throw fatal;
        }
        Site site = SITES.computeIfAbsent(where, key -> new Site());
        long now = System.currentTimeMillis();
        long count;
        long unlogged;
        boolean first;
        boolean summary;
        synchronized (site) {
            count = ++site.count;
            first = count == 1;
            summary = !first && now - site.lastLog >= SUMMARY_MS;
            unlogged = count - site.logged;
            if (first || summary) {
                site.lastLog = now;
                site.logged = count;
            }
        }
        if (first) {
            TimeMod.LOGGER.error("[TIME] Absturzschutz: Fehler in „{}“ abgefangen, das Spiel läuft weiter. Bitte mit diesem Log melden.",
                where, error);
        } else if (summary) {
            TimeMod.LOGGER.warn("[TIME] Absturzschutz: „{}“ – {} weitere Fehler abgefangen, zuletzt: {}", where, unlogged, error.toString());
        }
    }

    /** Wie oft an dieser Stelle schon ein Fehler abgefangen wurde (für Tests). */
    public static long count(String where) {
        Site site = SITES.get(where);
        if (site == null) {
            return 0;
        }
        synchronized (site) {
            return site.count;
        }
    }

    /** Insgesamt abgefangene Fehler (für Tests). */
    public static long total() {
        long total = 0;
        for (String where : SITES.keySet()) {
            total += count(where);
        }
        return total;
    }

    public static void run(String where, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            report(where, error);
        }
    }

    public static <T> T get(String where, Supplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (Throwable error) {
            report(where, error);
            return fallback;
        }
    }

    public static boolean test(String where, BooleanSupplier action, boolean fallback) {
        try {
            return action.getAsBoolean();
        } catch (Throwable error) {
            report(where, error);
            return fallback;
        }
    }

    public static int getInt(String where, IntSupplier action, int fallback) {
        try {
            return action.getAsInt();
        } catch (Throwable error) {
            report(where, error);
            return fallback;
        }
    }

    /**
     * Stammt dieser Fehler aus TIME? Gesucht wird nur in den Aufrufen, die tiefer liegen als die
     * Methode, die ihn gerade fängt und hier direkt fragt. Dass TIME weiter außen im Aufruf steht
     * (etwa weil die Zeit-Engine den Tick ausgelöst hat), zählt nicht.
     */
    public static boolean blames(Throwable error) {
        return blames(error, 1);
    }

    /**
     * Wie {@link #blames(Throwable)}, wenn zwischen der fangenden Methode und diesem Aufruf noch
     * {@code helpers} Hilfsmethoden liegen.
     */
    public static boolean blames(Throwable error, int helpers) {
        // Aufrufe von der fangenden Methode bis zum Anfang des Threads
        int outer = StackWalker.getInstance().walk(frames -> (int) frames.count()) - 1 - helpers;
        Throwable current = error;
        for (int depth = 0; current != null && depth < 16; depth++) {
            StackTraceElement[] trace = current.getStackTrace();
            int inner = trace.length - outer;
            if (inner <= 0 || trace.length >= MAX_TRACE) {
                inner = trace.length;
            }
            for (int i = 0; i < inner; i++) {
                if (isOurs(trace[i])) {
                    return true;
                }
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    /** Gehört dieser Aufruf zu TIME (eigene Klasse oder ein in Minecraft eingehängter Mixin)? */
    static boolean isOurs(StackTraceElement frame) {
        String method = frame.getMethodName();
        if (method.startsWith("mixinextras$")) {
            return false;
        }
        String owner = frame.getClassName();
        if (owner.startsWith(PACKAGE)) {
            return !owner.contains("$$Lambda");
        }
        return method.startsWith("timemod$") || method.contains("$timemod$");
    }

    /** Wirft einen gefangenen Fehler unverändert weiter, auch eine geprüfte Ausnahme. */
    public static RuntimeException rethrow(Throwable error) {
        throw Safety.<RuntimeException>sneaky(error);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneaky(Throwable error) throws E {
        throw (E) error;
    }
}
