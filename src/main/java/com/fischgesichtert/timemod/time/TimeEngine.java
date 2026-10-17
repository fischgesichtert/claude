package com.fischgesichtert.timemod.time;

import com.fischgesichtert.timemod.TimeMod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame Logik von Server- und Client-Engine: Zonenliste und die Frage
 * „welche Zone bestimmt die Zeit an dieser Stelle?“.
 */
public abstract class TimeEngine {
    /** Entity-Typen, die nie von Zeitmanipulation betroffen sind (per Datapack erweiterbar). */
    public static final TagKey<EntityType<?>> TIME_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, TimeMod.id("time_immune"));

    protected final Level level;
    protected final List<TimeZone> zones = new ArrayList<>();
    @Nullable
    protected TimeZone fullGlobal;

    protected TimeEngine(Level level) {
        this.level = level;
    }

    public Level level() {
        return level;
    }

    public final boolean isIdle() {
        return zones.isEmpty();
    }

    public List<TimeZone> zones() {
        return Collections.unmodifiableList(zones);
    }

    @Nullable
    public TimeZone fullGlobal() {
        return fullGlobal;
    }

    @Nullable
    public TimeZone zone(int id) {
        for (TimeZone zone : zones) {
            if (zone.id() == id) {
                return zone;
            }
        }
        return null;
    }

    protected void recomputeFullGlobal() {
        fullGlobal = null;
        for (TimeZone zone : zones) {
            if (zone.isFullGlobal() && (fullGlobal == null || zone.id() > fullGlobal.id())) {
                fullGlobal = zone;
            }
        }
    }

    /** Die maßgebliche Zone an einer Stelle für eine Kategorie, oder {@code null} (normale Zeit). */
    @Nullable
    public TimeZone resolve(double x, double y, double z, int category) {
        TimeZone best = null;
        for (int i = 0, n = zones.size(); i < n; i++) {
            TimeZone zone = zones.get(i);
            if (!zone.isTargeted() && zone.accepts(category) && zone.shape().contains(x, y, z) && (best == null || zone.outranks(best))) {
                best = zone;
            }
        }
        return best;
    }

    /** Wie {@link #resolve}, berücksichtigt aber Zonen, die nur auf eine bestimmte Entity wirken. */
    @Nullable
    protected TimeZone resolveFor(Entity entity, int category) {
        double x = entity.getX();
        double y = entity.getY(0.5);
        double z = entity.getZ();
        TimeZone best = null;
        for (int i = 0, n = zones.size(); i < n; i++) {
            TimeZone zone = zones.get(i);
            if (zone.isTargeted()) {
                if (zone.targetEntity() != entity.getId()) {
                    continue;
                }
            } else if (!zone.accepts(category) || !zone.shape().contains(x, y, z)) {
                continue;
            }
            if (best == null || zone.outranks(best)) {
                best = zone;
            }
        }
        return best;
    }

    /** Mitwandernde Zonen an die aktuelle Position ihrer Entity setzen (beide Seiten, jeden Tick). */
    public void updateFollowingZones() {
        for (TimeZone zone : zones) {
            if (!zone.isTargeted()) {
                continue;
            }
            Entity entity = level.getEntity(zone.targetEntity());
            if (entity != null) {
                double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.75 + 0.25;
                zone.moveTo(new ZoneShape.Sphere(entity.getBoundingBox().getCenter(), radius));
            }
        }
    }

    @Nullable
    public TimeZone resolveBlock(BlockPos pos, int category) {
        return resolve(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, category);
    }

    /**
     * Zone für eine Entity. Spieler und von Spielern gesteuerte Reittiere stehen außerhalb der Zeit,
     * ebenso alles mit dem Tag {@code #timemod:time_immune}.
     */
    @Nullable
    public TimeZone resolveEntity(Entity entity) {
        if (zones.isEmpty() || isOutsideTime(entity)) {
            return null;
        }
        return resolveFor(entity, TimeFilter.categoryOf(entity));
    }

    public static boolean isOutsideTime(Entity entity) {
        if (entity instanceof Player) {
            return true;
        }
        if (entity.getControllingPassenger() instanceof Player) {
            return true;
        }
        return entity.getType().is(TIME_IMMUNE);
    }

    /**
     * Spieler in einer fremden Stopp-Zone sind eingefroren: Sie können sich nicht bewegen, nicht
     * handeln und altern nicht. Der Erzeuger der Zone und Zuschauer sind ausgenommen.
     */
    public boolean isFrozenPlayer(Player player) {
        if (zones.isEmpty() || player.isSpectator()) {
            return false;
        }
        TimeZone zone = resolveFor(player, TimeFilter.PLAYERS);
        if (zone == null || !zone.isStop()) {
            return false;
        }
        // Beim Rückspulen läuft jeder mit zurück – auch wer es ausgelöst hat
        return zone.isRewind() || !player.getUUID().equals(zone.owner()) && !TimeImmunity.isImmune(player);
    }

    /** Steht diese Entity gerade im Stillstand? */
    public boolean isFrozen(Entity entity) {
        if (entity instanceof Player player) {
            return isFrozenPlayer(player);
        }
        TimeZone zone = resolveEntity(entity);
        return zone != null && zone.isStop();
    }

    /** Tickt eine Block-Entity im normalen Durchlauf – je nach Zone gar nicht, einmal oder öfter. */
    public abstract void tickBlockEntity(TickingBlockEntity ticker, Runnable original);

    /** Zeitfaktor an einer Position (für Klang und Partikel); 1 = normale Zeit. */
    public double factorAt(double x, double y, double z, int category) {
        TimeZone zone = resolve(x, y, z, category);
        return zone == null ? 1.0 : zone.factor();
    }
}
