package com.fischgesichtert.timemod.time;

import net.minecraft.server.level.ServerLevel;

/** Wird per Mixin von {@code LevelTicks} implementiert, damit die Engine geplante Ticks umleiten kann. */
public interface TimeTicks {
    void timemod$bind(ServerLevel level, boolean fluid);
}
