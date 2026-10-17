package com.fischgesichtert.timemod.item;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Worauf zielt der Spieler? Entities (auch Pfeile im Flug) vor Blöcken. */
public final class TimeTargeting {
    private TimeTargeting() {
    }

    @Nullable
    static Entity entity(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        AABB area = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(maxDist)), area,
            e -> !e.isSpectator() && e != player, maxDist * maxDist);
        return hit != null ? hit.getEntity() : null;
    }

    static Optional<BlockHitResult> block(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit) : Optional.empty();
    }

    /** Ziel-Mittelpunkt für Zonen: angeschauter Block oder ein Stück vor dem Spieler. */
    public static Vec3 point(Player player, double range) {
        return block(player, range).map(hit -> Vec3.atCenterOf(hit.getBlockPos().relative(hit.getDirection())))
            .orElseGet(() -> player.getEyePosition().add(player.getViewVector(1.0F).scale(Math.min(range, 6))));
    }
}
