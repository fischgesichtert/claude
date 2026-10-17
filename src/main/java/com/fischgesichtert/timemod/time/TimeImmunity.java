package com.fischgesichtert.timemod.time;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * Wer sich im angehaltenen Zustand anderer bewegen darf. Später kommen hier z. B. die
 * Chrono-Rüstung oder Amulette dazu; andere Mods können eigene Regeln registrieren.
 */
public final class TimeImmunity {
    private static final List<Predicate<Player>> RULES = new ArrayList<>();

    private TimeImmunity() {
    }

    public static void register(Predicate<Player> rule) {
        RULES.add(rule);
    }

    public static boolean isImmune(Player player) {
        for (Predicate<Player> rule : RULES) {
            if (rule.test(player)) {
                return true;
            }
        }
        return false;
    }
}
