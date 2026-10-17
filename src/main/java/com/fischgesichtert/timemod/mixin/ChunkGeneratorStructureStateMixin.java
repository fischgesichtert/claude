package com.fischgesichtert.timemod.mixin;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla berechnet die Strukturpositionen (128 Festungen samt Biomsuche) beim Erzeugen jeder Welt
 * auf dem Server-Thread an. Die Epochen holen das erst beim ersten Besuch nach – dann aber womöglich
 * von mehreren Generator-Threads gleichzeitig, deshalb ist die Berechnung hier abgesichert.
 */
@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin {
    @Shadow
    private boolean hasGeneratedPositions;

    @Unique
    private volatile boolean timemod$ready;

    @Shadow
    private void generatePositions() {
        throw new AssertionError();
    }

    @Inject(method = "ensureStructuresGenerated", at = @At("HEAD"), cancellable = true)
    private void timemod$threadSafe(CallbackInfo ci) {
        if (!timemod$ready) {
            synchronized (this) {
                if (!hasGeneratedPositions) {
                    generatePositions();
                    hasGeneratedPositions = true;
                }
                timemod$ready = true;
            }
        }
        ci.cancel();
    }
}
