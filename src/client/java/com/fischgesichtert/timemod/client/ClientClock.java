package com.fischgesichtert.timemod.client;

import net.minecraft.client.Minecraft;

/** Zählt Client-Ticks; Effekte laufen in Spielzeit (pausierbar, aufnahmefest) statt in Echtzeit. */
public final class ClientClock {
    private static long ticks;

    private ClientClock() {
    }

    public static void tick() {
        ticks++;
    }

    public static long ticks() {
        return ticks;
    }

    /** Sekunden Spielzeit inklusive Bruchteil des laufenden Ticks. */
    public static double seconds() {
        return (ticks + Minecraft.getInstance().getFrameTime()) / 20.0;
    }
}
