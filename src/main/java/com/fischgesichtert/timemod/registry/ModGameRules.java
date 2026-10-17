package com.fischgesichtert.timemod.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

/** Spielregeln von TIME (/gamerule …). */
public final class ModGameRules {
    /** Ob Zeitwächter Spielern mit hohem Paradox-Druck nachstellen. */
    public static final GameRules.Key<GameRules.BooleanValue> TIME_WARDENS =
        GameRuleRegistry.register("doTimeWardens", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));

    private ModGameRules() {
    }

    public static void init() {
        // Laden der Klasse registriert die Regeln.
    }
}
