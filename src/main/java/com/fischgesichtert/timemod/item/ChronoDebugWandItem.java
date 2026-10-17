package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entwickler-Werkzeug: Rechtsklick erzeugt dort, wo man hinschaut, eine Zeitzone (Radius 5, 30 s),
 * Schleichen + Rechtsklick wechselt den Faktor. Ein zweiter Rechtsklick ersetzt die alte Zone.
 */
public class ChronoDebugWandItem extends Item {
    public static final double[] FACTORS = {0.0, 0.1, 0.25, 0.5, 2.0, 4.0, 10.0};
    private static final String KEY = "FactorIndex";
    private static final double RADIUS = 5.0;
    private static final int DURATION = 30 * 20;

    public ChronoDebugWandItem(Properties properties) {
        super(properties);
    }

    public static double factor(ItemStack stack) {
        int index = stack.hasTag() ? stack.getTag().getInt(KEY) : 4;
        return FACTORS[Math.floorMod(index, FACTORS.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            int index = Math.floorMod((stack.hasTag() ? stack.getTag().getInt(KEY) : 4) + 1, FACTORS.length);
            stack.getOrCreateTag().putInt(KEY, index);
            serverPlayer.displayClientMessage(Component.translatable("item.timemod.chrono_debug_wand.factor", format(FACTORS[index])).withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
            return InteractionResultHolder.success(stack);
        }
        Vec3 center = target(player);
        ServerTimeEngine engine = ServerTimeEngine.of(serverLevel);
        engine.removeZonesOwnedBy(player.getUUID());
        double factor = factor(stack);
        // Ein Zeitstopp hält auch andere Spieler an (der Benutzer selbst bleibt beweglich)
        int mask = factor == 0.0 ? TimeFilter.DEFAULT | TimeFilter.PLAYERS : TimeFilter.DEFAULT;
        engine.addZone(new ZoneShape.Sphere(center, RADIUS), factor, mask, player.getUUID(), DURATION);
        serverPlayer.displayClientMessage(Component.translatable("item.timemod.chrono_debug_wand.created", format(factor)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.consume(stack);
    }

    private static Vec3 target(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(32));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return Vec3.atCenterOf(hit.getBlockPos().relative(hit.getDirection()));
        }
        return eye.add(player.getLookAngle().scale(6));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.timemod.chrono_debug_wand.factor", format(factor(stack))).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.timemod.chrono_debug_wand.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private static String format(double factor) {
        return factor == Math.rint(factor) ? (long) factor + "×" : String.format(Locale.ROOT, "%.2f×", factor);
    }
}
