package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Zeitsiegel – das Dienstsiegel eines Zeitwächters. Aufgedrückt versiegelt es die Risse, die man in
 * die eigene Zeitlinie gerissen hat: Der Paradox-Druck sinkt um 20 %.
 */
public class TimeSealItem extends Item {
    public static final float RELIEF = 20.0F;
    public static final int COOLDOWN = 10 * 20;

    public TimeSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        float pressure = ParadoxPressure.of(serverPlayer);
        if (pressure <= 0.0F) {
            player.displayClientMessage(Component.translatable("message.timemod.seal.calm").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        ParadoxPressure.relieve(serverPlayer, RELIEF);
        player.displayClientMessage(Component.translatable("message.timemod.seal.used",
            Math.round(ParadoxPressure.of(serverPlayer))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SEAL_STAMP, SoundSource.PLAYERS, 1.0F, 1.0F);
        ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.3, 0.4, 0.03);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
