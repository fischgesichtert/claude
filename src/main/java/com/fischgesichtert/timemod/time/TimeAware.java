package com.fischgesichtert.timemod.time;

/**
 * Für Entities und Block-Entities: Abstand der Eigenzeit zur Weltzeit in Ticks.
 * Wer schneller läuft als die Welt, ist „älter“ (positiver Wert), wer langsamer läuft, „jünger“.
 */
public interface TimeAware {
    long timemod$getEigenzeit();

    void timemod$setEigenzeit(long ticks);

    default void timemod$addEigenzeit(long ticks) {
        timemod$setEigenzeit(timemod$getEigenzeit() + ticks);
    }
}
