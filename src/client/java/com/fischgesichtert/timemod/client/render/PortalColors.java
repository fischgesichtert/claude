package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.era.Era;

/** Die Farbe der Toröffnung verrät das Ziel: kühles Cyan in die Zukunft, Gold in die Geschichte, Glut ins Erdmittelalter. */
public final class PortalColors {
    private PortalColors() {
    }

    public static int of(Era era) {
        return switch (era.age()) {
            case FUTURE -> 0x86F0FF;
            case PRESENT -> 0xFFF4D6;
            case HISTORY -> 0xFFD27A;
            case STONE_AGE -> 0xE8C090;
            case ICE_AGE -> 0xBCE4FF;
            case CENOZOIC -> 0xC8E07A;
            case MESOZOIC -> 0xFF9A5A;
        };
    }
}
