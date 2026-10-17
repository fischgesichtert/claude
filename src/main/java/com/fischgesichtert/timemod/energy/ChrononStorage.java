package com.fischgesichtert.timemod.energy;

/**
 * Block-Entities, die Chronon halten (Puffer und eingelegte Werkzeuge). Damit sieht die
 * Chronon-Bilanz beim Zurückspulen auch Chronon, das in Maschinen steckt.
 */
public interface ChrononStorage {
    /** Alles Chronon in diesem Block: Puffer plus eingelegte Items. */
    int storedChronon();

    /** Entnimmt bis zu {@code amount} (erst aus dem Puffer, dann aus Items); liefert die entnommene Menge. */
    int drainChronon(int amount);
}
