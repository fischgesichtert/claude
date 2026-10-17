package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Werkzeuge der Stufe I laufen mit Federspannung statt Chronon: Schleichen + Rechtsklick halten
 * zieht das Uhrwerk auf (es tickt dabei hörbar), jede Benutzung entspannt die Feder ein Stück.
 */
public abstract class WindUpItem extends Item {
    public static final String TENSION = "Tension";
    public static final int MAX_TENSION = 100;

    protected WindUpItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int tension(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TENSION) : 0;
    }

    public static void setTension(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TENSION, Mth.clamp(value, 0, MAX_TENSION));
    }

    /** Verbraucht Federspannung; im Kreativmodus gratis. */
    protected static boolean consume(Player player, ItemStack stack, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int t = tension(stack);
        if (t < amount) {
            player.displayClientMessage(Component.translatable("message.timemod.wind_up").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        setTension(stack, t - amount);
        return true;
    }

    /** Aufziehen beginnt mit Schleichen + Rechtsklick in die Luft (oder auf etwas, das nicht reagiert). */
    protected boolean startWinding(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        player.startUsingItem(hand);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (startWinding(player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return activate(level, player, stack, hand);
    }

    /** Benutzung ohne Ziel (Rechtsklick in die Luft, nicht schleichend). */
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, InteractionHand hand) {
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int held = getUseDuration(stack) - remaining;
        if (held % 3 != 0 || tension(stack) >= MAX_TENSION) {
            return;
        }
        setTension(stack, tension(stack) + 4);
        if (!level.isClientSide) {
            float pitch = 0.8F + 0.6F * tension(stack) / MAX_TENSION;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.CLOCK_TICK, SoundSource.PLAYERS, 0.5F, pitch);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * tension(stack) / MAX_TENSION);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0A030;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.timemod.tension", tension(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.timemod.wind_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
