package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zeitlupen-Pendel: Zone mit 0,5× oder 0,25× Zeit (Radius 6, 30 s). */
public class SlowPendulumItem extends ModeChrononToolItem {
    public static final double RADIUS = 6;
    public static final int DURATION = 30 * 20;
    public static final int COST = 100;

    public SlowPendulumItem(Properties properties) {
        super(4000, new double[] {0.5, 0.25}, properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, double factor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(player, stack, COST)) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine.of(serverLevel).addZone(new ZoneShape.Sphere(TimeTargeting.point(player, 24), RADIUS), factor, TimeFilter.DEFAULT, player.getUUID(), DURATION);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }
}
