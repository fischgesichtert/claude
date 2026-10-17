package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.safety.Safety;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Die Ruinenwächter wachen über ihre Ruine: Wer in ihrer Nähe Blöcke abbaut oder Truhen und andere
 * Behälter öffnet, weckt alle Wächter im Umkreis von 24 Blöcken – und sie greifen ihn an.
 */
public final class RuinWatch {
    public static final double RANGE = 24.0;

    private RuinWatch() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> Safety.run("Ruinenwächter: Alarm", () -> alarm(level, player, pos)));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide && !player.isSecondaryUseActive()) {
                Safety.run("Ruinenwächter: Alarm", () -> {
                    BlockEntity blockEntity = level.getBlockEntity(hit.getBlockPos());
                    if (blockEntity instanceof MenuProvider) {
                        alarm(level, player, hit.getBlockPos());
                    }
                });
            }
            return InteractionResult.PASS;
        });
    }

    /** Weckt die Wächter rund um {@code pos} und hetzt sie auf {@code player}; gibt zurück, wie viele es waren. */
    public static int alarm(Level level, Player player, BlockPos pos) {
        if (level.isClientSide) {
            return 0;
        }
        var guardians = level.getEntitiesOfClass(RuinGuardian.class, new AABB(pos).inflate(RANGE), RuinGuardian::isAlive);
        for (RuinGuardian guardian : guardians) {
            guardian.alarm(player);
        }
        return guardians.size();
    }
}
