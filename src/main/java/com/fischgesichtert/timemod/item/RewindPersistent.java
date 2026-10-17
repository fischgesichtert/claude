package com.fischgesichtert.timemod.item;

import net.minecraft.world.item.ItemStack;

/**
 * Items mit Daten, die ein Zurückspulen überstehen – etwa die Meisterschaft der Chronos-Uhr.
 * Erfahrung lässt sich nicht zurückspulen.
 */
public interface RewindPersistent {
    /** {@code restored} ersetzt gleich {@code current} im Inventar: Was davon soll bleiben? */
    void carryThroughRewind(ItemStack current, ItemStack restored);
}
