package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Taschenuhr: zeigt Uhrzeit, Tag, Mondphase und den Zeitfluss an der eigenen Position. */
public class PocketWatchItem extends Item {
    private static final String[] MOON = {"full", "waning_gibbous", "last_quarter", "waning_crescent", "new", "waxing_crescent", "first_quarter", "waxing_gibbous"};

    public PocketWatchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L + 1;
        int hours = (int) ((dayTime / 1000L + 6L) % 24L);
        int minutes = (int) ((dayTime % 1000L) * 60L / 1000L);
        MutableComponent line = Component.translatable("message.timemod.pocket_watch.time", day, String.format(Locale.ROOT, "%02d:%02d", hours, minutes),
            Component.translatable("moon.timemod." + MOON[level.getMoonPhase()]));
        var engine = ((TimeLevel) level).timemod$getEngine();
        TimeZone zone = engine == null ? null : engine.resolve(player.getX(), player.getY(), player.getZ(), TimeFilter.ALL_BLOCKS | TimeFilter.ALL_ENTITIES);
        if (zone != null) {
            line.append(" · ").append(Component.translatable(zone.isStop() ? "message.timemod.pocket_watch.stopped" : "message.timemod.pocket_watch.flow",
                String.format(Locale.ROOT, "%.2f", zone.factor())).withStyle(ChatFormatting.AQUA));
        }
        player.displayClientMessage(line.withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CLOCK_TICK, SoundSource.PLAYERS, 0.6F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
