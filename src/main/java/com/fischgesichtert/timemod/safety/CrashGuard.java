package com.fischgesichtert.timemod.safety;

import com.fischgesichtert.timemod.TimeMod;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Entscheidet an den Stellen, an denen Minecraft einen Fehler sonst zum Absturz macht (Entity-Tick,
 * Block-Entity-Tick, Spieler-Tick, Block-Ticks), ob er von TIME kommt und abgefangen wird.
 * Eine eigene Entity, die dauerhaft scheitert, wird nach einigen Versuchen entfernt, statt die Welt
 * jeden Tick aufs Neue zu stören.
 */
public final class CrashGuard {
    /** Nach so vielen gescheiterten Ticks wird eine TIME-Entity entfernt (nie ein Spieler). */
    static final int DISCARD_AFTER = 40;
    private static final Map<Entity, Integer> FAILURES = Collections.synchronizedMap(new WeakHashMap<>());

    private CrashGuard() {
    }

    private static boolean ours(ResourceLocation id) {
        return id != null && TimeMod.MOD_ID.equals(id.getNamespace());
    }

    /**
     * Ein Entity-Tick ist gescheitert. {@code true} = abgefangen (der Tick fällt aus), {@code false}
     * = nicht von TIME, Minecraft soll den Fehler wie gewohnt behandeln. Aufruf direkt aus dem
     * fangenden Mixin.
     */
    public static boolean absorbEntity(Entity entity, Throwable error) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean own = ours(type);
        if (!own && !Safety.blames(error, 1)) {
            return false;
        }
        Safety.report("Entity-Tick " + type, error);
        if (own && !(entity instanceof Player) && !entity.level().isClientSide) {
            int failures = FAILURES.merge(entity, 1, Integer::sum);
            if (failures >= DISCARD_AFTER) {
                FAILURES.remove(entity);
                TimeMod.LOGGER.error("[TIME] Absturzschutz: {} scheitert dauerhaft und wird entfernt.", entity);
                Safety.run("Entity entfernen " + type, entity::discard);
            }
        }
        return true;
    }

    /** Ein Block-Entity-Tick ist gescheitert. Aufruf direkt aus dem fangenden Mixin. */
    public static boolean absorbBlockEntity(BlockEntity blockEntity, Throwable error) {
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (!ours(type) && !Safety.blames(error, 1)) {
            return false;
        }
        Safety.report("Block-Entity-Tick " + type, error);
        return true;
    }

    /** Irgendein anderer Aufruf ist gescheitert (Block-Tick, Spieler-Tick …). Aufruf direkt aus dem fangenden Mixin. */
    public static boolean absorb(String where, Throwable error) {
        if (!Safety.blames(error, 1)) {
            return false;
        }
        Safety.report(where, error);
        return true;
    }
}
