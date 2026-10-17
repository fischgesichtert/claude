package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Moment-Linse: hält einen Moment fest – ein Wesen, ein Geschoss im Flug oder einen einzelnen Block
 * (z. B. flackerndes Chronit-Erz) für 3 Sekunden.
 */
public class MomentLensItem extends WindUpItem {
    public static final int DURATION = 3 * 20;
    public static final int COST = 25;

    public MomentLensItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, InteractionHand hand) {
        Entity target = TimeTargeting.entity(player, 24);
        if (target == null || target instanceof Player) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            if (!consume(player, stack, COST)) {
                return InteractionResultHolder.fail(stack);
            }
            ServerTimeEngine.of(serverLevel).addEntityZone(target, 0.0, TimeFilter.ALL_ENTITIES & ~TimeFilter.PLAYERS, player.getUUID(), DURATION);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown() || target instanceof Player) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            ItemStack held = player.getItemInHand(hand);
            if (!consume(player, held, COST)) {
                return InteractionResult.FAIL;
            }
            ServerTimeEngine.of(serverLevel).addEntityZone(target, 0.0, TimeFilter.ALL_ENTITIES & ~TimeFilter.PLAYERS, player.getUUID(), DURATION);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!consume(player, context.getItemInHand(), COST)) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos();
        ServerTimeEngine.of(level).addZone(new ZoneShape.Box(new AABB(pos)), 0.0, TimeFilter.ALL_BLOCKS, player.getUUID(), DURATION);
        return InteractionResult.CONSUME;
    }
}
