package com.fischgesichtert.timemod.time;

import org.jetbrains.annotations.Nullable;

/** Wird per Mixin von {@code Level} implementiert. */
public interface TimeLevel {
    @Nullable
    TimeEngine timemod$getEngine();

    void timemod$setEngine(TimeEngine engine);
}
