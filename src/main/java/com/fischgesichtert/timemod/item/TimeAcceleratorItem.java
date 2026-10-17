package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zeitraffer: Zone mit 2–4× Zeit (Radius 6, 30 s), dort wo man hinschaut. */
public class TimeAcceleratorItem extends ModeChrononToolItem {
    public static final double RADIUS = 6;
    public static final int DURATION = 30 * 20;

    public TimeAcceleratorItem(Properties properties) {
        super(6000, new double[] {2.0, 3.0, 4.0}, properties);
    }

    public static int cost(double factor) {
        return (int) Math.round(150 * (factor - 1) * (factor - 1));
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, double factor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(player, stack, cost(factor))) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine.of(serverLevel).addZone(new ZoneShape.Sphere(TimeTargeting.point(player, 24), RADIUS), factor, TimeFilter.DEFAULT, player.getUUID(), DURATION);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }
}
