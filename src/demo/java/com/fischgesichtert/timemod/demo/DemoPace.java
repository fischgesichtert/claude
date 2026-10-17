package com.fischgesichtert.timemod.demo;

/** Takt der Demo: Länge eines Spiel-Ticks in Millisekunden (normal 50). */
public final class DemoPace {
    public static final long TICK_MS = Long.getLong("timemod.demo.tickMs", 50L);

    private DemoPace() {
    }
}
