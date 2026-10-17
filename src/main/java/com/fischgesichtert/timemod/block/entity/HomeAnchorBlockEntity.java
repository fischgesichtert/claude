package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Der Heimkehr-Anker merkt sich, woher die Reise kam: Ein Klick, und der Zeitstrom trägt dich zurück
 * vor das Tor. Ein selbst gebauter Anker kennt kein Tor – er führt in die Gegenwart an dieselbe Stelle.
 */
public class HomeAnchorBlockEntity extends BlockEntity {
    @Nullable
    private ResourceKey<Level> returnLevel;
    @Nullable
    private BlockPos returnPos;

    public HomeAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOME_ANCHOR, pos, state);
    }

    public void setReturn(ResourceKey<Level> level, BlockPos pos) {
        returnLevel = level;
        returnPos = pos.immutable();
        setChanged();
    }

    public boolean leadsTo(ResourceKey<Level> level, BlockPos pos) {
        return level.equals(returnLevel) && returnPos != null && returnPos.closerThan(pos, 8);
    }

    /** Das Ziel der Heimkehr: das Tor, durch das man kam, sonst die Gegenwart. */
    public Era destination() {
        Era era = returnLevel == null ? null : Eras.byLevel(returnLevel);
        return era == null ? Eras.PRESENT : era;
    }

    public void use(ServerPlayer player) {
        Era here = Eras.byLevel(player.level().dimension());
        Era target = destination();
        if (here == null || target == here) {
            player.displayClientMessage(Component.translatable("message.timemod.anchor.here").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BlockPos landing = returnPos != null && target.level().equals(returnLevel) ? returnPos : worldPosition;
        if (!TimeStream.depart(player, target, landing, worldPosition, false)) {
            player.displayClientMessage(Component.translatable("message.timemod.anchor.wait").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        if (returnLevel != null && returnPos != null) {
            tag.putString("ReturnLevel", returnLevel.location().toString());
            tag.put("ReturnPos", NbtUtils.writeBlockPos(returnPos));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("ReturnLevel") && tag.contains("ReturnPos")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("ReturnLevel"));
            returnLevel = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            returnPos = NbtUtils.readBlockPos(tag.getCompound("ReturnPos"));
        }
    }
}
