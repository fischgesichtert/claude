package com.fischgesichtert.timemod.time;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Bereich, in dem die Zeit mit einem eigenen Faktor läuft.
 * <p>
 * Der Faktor wirkt nicht über Geschwindigkeits-Effekte, sondern darüber, wie oft die Spiellogik
 * selbst gerechnet wird: Bei 2× wird alles im Bereich pro echtem Tick zweimal komplett getickt,
 * bei 0,5× nur jeden zweiten Tick, bei 0× gar nicht. Bruchteile laufen über einen Akkumulator.
 */
public final class TimeZone {
    public static final double MAX_FACTOR = 10.0;

    private final int id;
    private final ZoneShape shape;
    private final double factor;
    private final int mask;
    @Nullable
    private final UUID owner;
    /**
     * Rückspul-Zone: Auf dem Server steht alles darin still (die Wiedergabe bewegt es), alle
     * Spieler darin werden mitgespult. Der Client zeigt sie als laufendes Band.
     */
    private final boolean rewind;
    /** Verbleibende echte Ticks; negativ = unbegrenzt. */
    private int remainingTicks;

    private double accumulator;
    /** Wie viele Spiel-Ticks der Inhalt im laufenden Welt-Schritt bekommt (0 = eingefroren). */
    private int steps = 1;
    /** Eigene Uhr der Zone für geplante Ticks: zählt jeden ausgeführten Schritt. */
    private long clock;

    @Nullable
    final ZoneTicks<Block> blockTicks;
    @Nullable
    final ZoneTicks<Fluid> fluidTicks;

    public TimeZone(int id, ZoneShape shape, double factor, int mask, @Nullable UUID owner, int durationTicks, boolean withTickQueues) {
        this(id, shape, factor, mask, owner, durationTicks, withTickQueues, false);
    }

    public TimeZone(int id, ZoneShape shape, double factor, int mask, @Nullable UUID owner, int durationTicks, boolean withTickQueues, boolean rewind) {
        this.id = id;
        this.shape = shape;
        this.rewind = rewind;
        this.factor = rewind ? 0.0 : Math.max(0.0, Math.min(MAX_FACTOR, factor));
        this.mask = shape.isGlobal() ? mask : mask & ~TimeFilter.ALL_WORLD;
        this.owner = owner;
        this.remainingTicks = durationTicks;
        this.blockTicks = withTickQueues ? new ZoneTicks<>() : null;
        this.fluidTicks = withTickQueues ? new ZoneTicks<>() : null;
    }

    public int id() {
        return id;
    }

    public ZoneShape shape() {
        return shape;
    }

    public double factor() {
        return factor;
    }

    public boolean isStop() {
        return factor == 0.0;
    }

    public boolean isRewind() {
        return rewind;
    }

    public int mask() {
        return mask;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public boolean accepts(int category) {
        return (mask & category) != 0;
    }

    /**
     * Globale Zone, die die komplette Welt-Simulation erfasst. Sie wird über die Welt-Uhr selbst
     * umgesetzt (zusätzliche bzw. ausgelassene Welt-Ticks) statt über einzelne Teilschritte.
     */
    public boolean isFullGlobal() {
        int worldSim = (TimeFilter.ALL_ENTITIES & ~TimeFilter.PLAYERS) | TimeFilter.ALL_BLOCKS | TimeFilter.ALL_WORLD;
        return shape.isGlobal() && (mask & worldSim) == worldSim;
    }

    /** Rangfolge bei Überschneidung: Rückspulen, dann Stopp, sonst gewinnt die neuere Zone. */
    public boolean outranks(TimeZone other) {
        if (rewind != other.rewind) {
            return rewind;
        }
        if (isStop() != other.isStop()) {
            return isStop();
        }
        if (shape.isGlobal() != other.shape.isGlobal()) {
            return !shape.isGlobal();
        }
        return id > other.id;
    }

    /** Schaltet den Akkumulator um einen Welt-Schritt weiter und liefert die Anzahl Spiel-Ticks. */
    public int advance() {
        return advance(factor);
    }

    /** Wie {@link #advance()}, aber mit einem abweichenden (z. B. verschachtelten) Faktor. */
    public int advance(double effectiveFactor) {
        accumulator += effectiveFactor;
        int s = (int) Math.floor(accumulator + 1.0E-9);
        accumulator -= s;
        if (accumulator < 0) {
            accumulator = 0;
        }
        steps = s;
        return s;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int steps() {
        return steps;
    }

    /** Rest des Akkumulators: wie weit der nächste Spiel-Tick schon „angebrochen“ ist (0..1). */
    public double phase() {
        return accumulator;
    }

    /** Übernimmt den Laufzustand einer älteren Kopie derselben Zone (Client-Synchronisation). */
    public void copyStateFrom(TimeZone other) {
        this.accumulator = other.accumulator;
        this.steps = other.steps;
    }

    public long clock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public long tickClock() {
        return ++clock;
    }

    /**
     * Zählt die Laufzeit zu Beginn eines echten Ticks herunter; {@code true}, sobald die Zone
     * abgelaufen ist. Eine Dauer von N wirkt damit auf genau N Ticks.
     */
    public boolean countDown() {
        if (remainingTicks < 0) {
            return false;
        }
        if (remainingTicks == 0) {
            return true;
        }
        remainingTicks--;
        return false;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        shape.write(buf);
        buf.writeDouble(factor);
        buf.writeVarInt(mask);
        buf.writeVarInt(remainingTicks);
        buf.writeBoolean(rewind);
        buf.writeBoolean(owner != null);
        if (owner != null) {
            buf.writeUUID(owner);
        }
    }

    public static TimeZone read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        ZoneShape shape = ZoneShape.read(buf);
        double factor = buf.readDouble();
        int mask = buf.readVarInt();
        int remaining = buf.readVarInt();
        boolean rewind = buf.readBoolean();
        UUID owner = buf.readBoolean() ? buf.readUUID() : null;
        return new TimeZone(id, shape, factor, mask, owner, remaining, false, rewind);
    }

    @Override
    public String toString() {
        return "TimeZone#" + id + "[" + shape + ", " + (rewind ? "rewind" : factor + "x") + ", mask=" + Integer.toBinaryString(mask) + "]";
    }
}
