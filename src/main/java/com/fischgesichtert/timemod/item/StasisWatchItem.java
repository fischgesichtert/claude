package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stillstand-Uhr: hält die Zeit im Umkreis von 12 Blöcken für 2 Sekunden an. Nur du bewegst dich. */
public class StasisWatchItem extends ChrononToolItem {
    public static final double RADIUS = 12;
    public static final int DURATION = 2 * 20;
    public static final int COST = 400;
    public static final int COOLDOWN = 10 * 20;

    public StasisWatchItem(Properties properties) {
        super(4000, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(player, stack, COST)) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine.of(serverLevel).addZone(new ZoneShape.Sphere(player.getEyePosition(), RADIUS), 0.0,
            TimeFilter.DEFAULT | TimeFilter.PLAYERS, player.getUUID(), DURATION);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }
}
