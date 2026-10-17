package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bremsfeder: ein Wesen (oder Geschoss) läuft 10 s lang mit halber Zeit. */
public class BrakeSpringItem extends WindUpItem {
    public static final double FACTOR = 0.5;
    public static final int DURATION = 10 * 20;
    public static final int COST = 15;

    public BrakeSpringItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, InteractionHand hand) {
        Entity target = TimeTargeting.entity(player, 16);
        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }
        return apply(level, player, stack, target) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return apply(player.level(), player, player.getItemInHand(hand), target) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private boolean apply(Level level, Player player, ItemStack stack, Entity target) {
        if (target instanceof Player || !(level instanceof ServerLevel serverLevel)) {
            return level.isClientSide;
        }
        if (!consume(player, stack, COST)) {
            return false;
        }
        ServerTimeEngine.of(serverLevel).addEntityZone(target, FACTOR, TimeFilter.ALL_ENTITIES & ~TimeFilter.PLAYERS, player.getUUID(), DURATION);
        return true;
    }
}
