package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.ChangeTracking;
import com.fischgesichtert.timemod.safety.Safety;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Eine Explosion in der Gegenwart verändert jeden Chunk, den sie trifft – auch die Zukunft dort. */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract List<BlockPos> getToBlow();

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void timemod$trackChange(boolean spawnParticles, CallbackInfo ci) {
        if (level.isClientSide || getToBlow().isEmpty()) {
            return;
        }
        Safety.run("Zukunft: Explosion merken", () -> {
            LongOpenHashSet chunks = new LongOpenHashSet();
            for (BlockPos pos : getToBlow()) {
                if (chunks.add(ChunkPos.asLong(pos))) {
                    ChangeTracking.changed(level, pos, false);
                }
            }
        });
    }
}
