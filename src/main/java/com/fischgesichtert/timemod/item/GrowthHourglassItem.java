package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Wachstums-Sanduhr: ein Block (Pflanze, Setzling, Ofen, Mod-Maschine) läuft 30 s lang 4× so schnell. */
public class GrowthHourglassItem extends WindUpItem {
    public static final double FACTOR = 4.0;
    public static final int DURATION = 30 * 20;
    public static final int COST = 20;

    public GrowthHourglassItem(Properties properties) {
        super(properties);
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
        ItemStack stack = context.getItemInHand();
        if (!consume(player, stack, COST)) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos();
        int mask = TimeFilter.ALL_BLOCKS;
        ServerTimeEngine.of(level).addZone(new ZoneShape.Box(new AABB(pos)), FACTOR, mask, player.getUUID(), DURATION);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        return InteractionResult.CONSUME;
    }
}
