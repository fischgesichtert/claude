package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rückspul-Spule: spult den Umkreis von 16 Blöcken bis zu 10 Sekunden zurück, sichtbar mit
 * doppelter Geschwindigkeit. Solange jemand eine Spule bei sich trägt, zeichnet die Welt auf.
 */
public class RewindSpoolItem extends ChrononToolItem implements RewindCapable {
    public static final double RADIUS = 16;
    public static final int TICKS = 10 * 20;
    public static final int COST = 800;

    public RewindSpoolItem(Properties properties) {
        super(4000, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        ChronoJournal journal = ServerTimeEngine.of(serverLevel).journal();
        if (!journal.isRecording() || journal.availableTicks() < 10 || journal.session() != null) {
            player.displayClientMessage(Component.translatable("message.timemod.rewind.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!pay(player, stack, COST)) {
            return InteractionResultHolder.fail(stack);
        }
        journal.rewind(TICKS, 2, new RewindSession.Region(player.position(), RADIUS), player.getUUID());
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }
}
