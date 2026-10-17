package com.fischgesichtert.timemod.time;

/**
 * Per Mixin an {@code LivingEntity}: Treffer im Zeitstopp werden gesammelt und erst wirksam,
 * wenn die Zeit für das Opfer weiterläuft – dann alle auf einmal.
 */
public interface TimeHitQueue {
    boolean timemod$hasPendingHits();

    /** Wendet alle gesammelten Treffer an (ohne Unverwundbarkeits-Pause dazwischen). */
    void timemod$releaseHits();

    /** Werden gerade die im Stillstand gesammelten Treffer ausgeteilt? (für Bosse, die das spüren) */
    boolean timemod$isReleasingHits();
}
