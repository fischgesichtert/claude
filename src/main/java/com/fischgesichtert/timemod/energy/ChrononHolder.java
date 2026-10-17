package com.fischgesichtert.timemod.energy;

import net.minecraft.world.item.ItemStack;

/** Items, die Chronon speichern können. */
public interface ChrononHolder {
    int chrononCapacity(ItemStack stack);
}
