package com.fischgesichtert.timemod.time;

/**
 * Darstellungszustand für den Client (per Mixin an Entities): mit welchem Faktor das Objekt im
 * letzten Tick lief und wie weit sein nächster Tick schon angebrochen ist. Daraus wird der
 * Interpolations-Fortschritt berechnet, damit Zeitlupe weich und Stopp wirklich still ist.
 */
public interface RenderTimeState {
    float timemod$getRenderRate();

    float timemod$getRenderPhase();

    void timemod$setRenderTime(float rate, float phase);

    static float remap(float partialTick, float rate, float phase) {
        if (rate >= 1.0F) {
            return partialTick;
        }
        float p = phase + partialTick * rate;
        return p > 1.0F ? 1.0F : p;
    }
}
