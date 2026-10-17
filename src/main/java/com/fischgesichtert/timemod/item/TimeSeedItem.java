package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.era.Timeline;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zeitsaat: Auf einen Setzling in der Vergangenheit gestreut, gibt sie ihm alle Jahre bis heute – in der
 * Gegenwart steht an derselben Stelle ein Baum, der so alt ist wie die Reise weit war.
 */
public class TimeSeedItem extends Item {
    public TimeSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        Player player = context.getPlayer();
        if (!state.is(BlockTags.SAPLINGS)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Era era = Eras.byLevel(level.dimension());
        if (era == null || !era.isPast()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.timemod.seed.not_past").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.FAIL;
        }
        Timeline timeline = Timeline.get(level.getServer());
        if (timeline.has(Timeline.Kind.TREE, pos)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.timemod.seed.already").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        CompoundTag data = new CompoundTag();
        data.putString("Sapling", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        timeline.add(level.getServer(), new Timeline.Entry(Timeline.Kind.TREE, pos, -(long) era.years(), data));
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.2, 0.5, 0.2, 0.02);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.timemod.seed.planted", era.span()).withStyle(ChatFormatting.GREEN), true);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "time_seed");
        }
        return InteractionResult.CONSUME;
    }
}
