package com.fischgesichtert.timemod.client;

/** Per Mixin an {@code WalkAnimationState}: Laufanimation über mehrere Teilschritte glätten. */
public interface WalkAnimationAccess {
    float timemod$speedOld();

    void timemod$setSpeedOld(float speedOld);

    /** Gesamter Fortschritt der Beinbewegung im letzten Tick; {@code NaN} = normaler Tick. */
    void timemod$setRenderDelta(float delta);
}
