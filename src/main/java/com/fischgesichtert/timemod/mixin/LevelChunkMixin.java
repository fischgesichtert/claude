package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zeitjournal: Jede Blockänderung der Welt läuft hier durch – auch die anderer Mods. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void timemod$recordBlockChange(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (level instanceof ServerLevel serverLevel) {
            ServerTimeEngine engine = ServerTimeEngine.of(serverLevel);
            if (engine != null) {
                engine.journal().onBlockChange((LevelChunk) (Object) this, pos, state);
            }
        }
    }
}
