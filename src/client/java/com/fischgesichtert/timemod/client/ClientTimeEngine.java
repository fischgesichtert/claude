package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.time.RenderTimeState;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Client-Seite der Zeit-Engine. Der Server ist maßgeblich; der Client rechnet die Zonen mit,
 * damit Animationen, Interpolation, Partikel und Klänge im selben Tempo laufen.
 */
public final class ClientTimeEngine extends TimeEngine {
    private boolean reentrant;

    public ClientTimeEngine(ClientLevel level) {
        super(level);
    }

    public void setZones(List<TimeZone> newZones) {
        for (TimeZone zone : newZones) {
            TimeZone old = zone(zone.id());
            if (old != null) {
                zone.copyStateFrom(old);
            }
        }
        zones.clear();
        zones.addAll(newZones);
        recomputeFullGlobal();
    }

    /** Effektiver Faktor einer Zone; lokale Zonen in einer globalen Zone multiplizieren sich. */
    public double effectiveFactor(TimeZone zone) {
        if (fullGlobal != null && zone != fullGlobal && !zone.isStop()) {
            return zone.factor() * fullGlobal.factor();
        }
        return zone.factor();
    }

    public void beginTick() {
        for (TimeZone zone : zones) {
            zone.advance(effectiveFactor(zone));
        }
    }

    /** Wie oft globale Welt-Systeme (Himmel, Wolken, Texturen) in diesem Client-Tick laufen. */
    public int worldSteps(int category) {
        TimeZone best = null;
        for (TimeZone zone : zones) {
            if (zone.shape().isGlobal() && zone.accepts(category) && (best == null || zone.outranks(best))) {
                best = zone;
            }
        }
        return best == null ? 1 : best.steps();
    }

    @Nullable
    public TimeZone zoneForEntity(Entity entity) {
        return resolveEntity(entity);
    }

    /**
     * Tickt eine Entity im Takt ihrer Zone. Nach mehreren Teilschritten werden die
     * Interpolations-Startwerte auf den Stand vor dem Tick gesetzt, damit die Bewegung über den
     * ganzen Tick gleitet statt zu springen.
     */
    public void tickEntity(Entity entity, Runnable vanillaTick) {
        TimeZone zone = resolveEntity(entity);
        float rate = zone == null ? 1.0F : (float) effectiveFactor(zone);
        float phase = zone == null ? 0.0F : (float) zone.phase();
        setRenderTime(entity, rate, phase);
        int steps = zone == null ? 1 : zone.steps();
        if (steps == 0) {
            return;
        }
        Snapshot before = steps > 1 ? Snapshot.of(entity) : null;
        reentrant = true;
        try {
            for (int i = 0; i < steps && !entity.isRemoved(); i++) {
                vanillaTick.run();
            }
        } finally {
            reentrant = false;
        }
        if (before != null) {
            before.applyAsOld(entity);
        }
    }

    /** {@code true}, während die Engine selbst Vanilla-Ticks auslöst. */
    public boolean isReentrant() {
        return reentrant;
    }

    private static void setRenderTime(Entity entity, float rate, float phase) {
        ((RenderTimeState) entity).timemod$setRenderTime(rate, phase);
        for (Entity passenger : entity.getPassengers()) {
            setRenderTime(passenger, rate, phase);
        }
    }

    /** Interpolations-Fortschritt für die Darstellung einer Entity in diesem Frame. */
    public float remapPartialTick(Entity entity, float partialTick) {
        if (zones.isEmpty()) {
            return partialTick;
        }
        RenderTimeState render = (RenderTimeState) entity;
        return RenderTimeState.remap(partialTick, render.timemod$getRenderRate(), render.timemod$getRenderPhase());
    }

    @Override
    public void tickBlockEntity(TickingBlockEntity ticker, Runnable original) {
        TimeZone zone = resolveBlock(ticker.getPos(), TimeFilter.MACHINES);
        int steps = zone == null ? 1 : zone.steps();
        for (int i = 0; i < steps; i++) {
            original.run();
        }
    }

    /** Wie oft ein Partikel an dieser Stelle in diesem Tick tickt. */
    public int particleSteps(double x, double y, double z) {
        TimeZone zone = resolve(x, y, z, TimeFilter.ENVIRONMENT);
        return zone == null ? 1 : zone.steps();
    }

    @Nullable
    public TimeZone particleZone(double x, double y, double z) {
        return resolve(x, y, z, TimeFilter.ENVIRONMENT);
    }

    /** Zeitfaktor für Klänge an einer Position. */
    public double soundFactor(double x, double y, double z) {
        TimeZone zone = resolve(x, y, z, TimeFilter.ALL_ENTITIES | TimeFilter.ALL_BLOCKS);
        return zone == null ? 1.0 : effectiveFactor(zone);
    }

    /** Werte vor einem Mehrfach-Tick, die danach als Interpolations-Start dienen. */
    private record Snapshot(double x, double y, double z, float yRot, float xRot, float walkDist,
                            float bodyRot, float headRot, float attackAnim, float animStep, float run,
                            float walkPosition, float walkSpeed, boolean living) {
        static Snapshot of(Entity entity) {
            if (entity instanceof LivingEntity living) {
                return new Snapshot(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), entity.walkDist,
                    living.yBodyRot, living.yHeadRot, living.attackAnim, living.animStep, living.run,
                    living.walkAnimation.position(), living.walkAnimation.speed(), true);
            }
            return new Snapshot(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), entity.walkDist,
                0, 0, 0, 0, 0, 0, 0, false);
        }

        void applyAsOld(Entity entity) {
            entity.xo = x;
            entity.yo = y;
            entity.zo = z;
            entity.xOld = x;
            entity.yOld = y;
            entity.zOld = z;
            entity.yRotO = yRot;
            entity.xRotO = xRot;
            entity.walkDistO = walkDist;
            if (living && entity instanceof LivingEntity livingEntity) {
                livingEntity.yBodyRotO = bodyRot;
                livingEntity.yHeadRotO = headRot;
                livingEntity.oAttackAnim = attackAnim;
                livingEntity.animStepO = animStep;
                livingEntity.oRun = run;
                WalkAnimationAccess walk = (WalkAnimationAccess) livingEntity.walkAnimation;
                walk.timemod$setSpeedOld(walkSpeed);
                walk.timemod$setRenderDelta(livingEntity.walkAnimation.position() - walkPosition);
            }
        }
    }
}
