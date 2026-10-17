package com.fischgesichtert.timemod.era;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Eine Zielzeit der Zeitmaschine: ein Jahr relativ zur Gegenwart und die Dimension, in der es liegt.
 * Wer dasselbe Jahr wählt, landet in derselben Welt – so treffen sich Spieler in der Zeit.
 *
 * @param years Jahre ab heute: negativ in der Vergangenheit, 0 für die Gegenwart
 * @param age   das Zeitalter (Schnellsprung, Fokus, Lebewelt)
 * @param level die Dimension dieser Epoche
 * @param mood  Licht und Luft der Epoche
 */
public record Era(int years, Age age, ResourceKey<Level> level, Mood mood) {
    /** Zeitalter in zeitlicher Reihenfolge, vom ältesten zum jüngsten. */
    public enum Age {
        MESOZOIC, CENOZOIC, ICE_AGE, STONE_AGE, HISTORY, PRESENT, FUTURE
    }

    /**
     * Die Stimmung einer Epoche: Nebeltönung als RGB, wie stark sie wirkt, und wo der Dunst
     * einsetzt (Anteil der Sichtweite; 1 = kein zusätzlicher Dunst).
     */
    public record Mood(int tint, float strength, float hazeStart) {
        public static final Mood CLEAR = new Mood(0xFFFFFF, 0.0F, 1.0F);

        public boolean isClear() {
            return strength <= 0.0F && hazeStart >= 1.0F;
        }
    }

    /** Kurzname für Schlüssel und Dimension: {@code p100}, {@code m66000000} oder {@code present}. */
    public String id() {
        return idOf(years);
    }

    static String idOf(int years) {
        return years == 0 ? "present" : (years > 0 ? "p" : "m") + Math.abs(years);
    }

    public boolean isPresent() {
        return years == 0;
    }

    public boolean isFuture() {
        return years > 0;
    }

    public boolean isPast() {
        return years < 0;
    }

    /** Ab einer Million Jahren in der Vergangenheit passt kein Vanilla-Wesen mehr in die Welt. */
    public boolean vanillaLife() {
        return years > -1_000_000;
    }

    /** Der Name der Epoche, etwa „Späte Kreidezeit“. */
    public Component title() {
        return Component.translatable("era.timemod." + id());
    }

    /** Das Jahr als Text, etwa „−70.000.000 Jahre“. */
    public Component yearText() {
        return Component.translatable("era.timemod." + id() + ".year");
    }

    /** Der Abstand zur Gegenwart ohne Vorzeichen, etwa „1.000 Jahre“ (für „… später“ oder „… alt“). */
    public Component span() {
        return Component.translatable("era.timemod." + id() + ".span");
    }
}
