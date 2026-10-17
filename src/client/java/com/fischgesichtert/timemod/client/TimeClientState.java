package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.time.TimeLevel;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/** Bequemer Zugriff auf die Client-Engine der aktuellen Welt. */
public final class TimeClientState {
    private TimeClientState() {
    }

    @Nullable
    public static ClientTimeEngine engine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && ((TimeLevel) mc.level).timemod$getEngine() instanceof ClientTimeEngine engine) {
            return engine;
        }
        return null;
    }

    public static boolean isLocalPlayerFrozen() {
        ClientTimeEngine engine = engine();
        return engine != null && engine.isLocalPlayerFrozen();
    }
}
