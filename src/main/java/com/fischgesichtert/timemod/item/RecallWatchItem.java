package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.rewind.PlayerHistory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Rückblick-Uhr: springt 5 Sekunden zurück – Weg rückwärts, Leben wie damals. */
public class RecallWatchItem extends WindUpItem {
    public static final int SECONDS = 5;
    public static final int COST = 40;
    public static final int COOLDOWN = 60 * 20;

    public RecallWatchItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!PlayerHistory.canRecall(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!consume(player, stack, COST)) {
            return InteractionResultHolder.fail(stack);
        }
        PlayerHistory.startRecall(serverPlayer, SECONDS * 20);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REWIND, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }
}
