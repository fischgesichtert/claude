package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeContext;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin implements TimeLevel {
    @Unique
    private TimeEngine timemod$engine;

    @Override
    public TimeEngine timemod$getEngine() {
        return timemod$engine;
    }

    @Override
    public void timemod$setEngine(TimeEngine engine) {
        timemod$engine = engine;
    }

    /** Eigenzeit: Objekte in Zeitzonen sehen ihre eigene Spielzeit. */
    @ModifyReturnValue(method = "getGameTime", at = @At("RETURN"))
    private long timemod$eigenzeit(long gameTime) {
        return TimeContext.adjust((Level) (Object) this, gameTime);
    }

    /**
     * Vanilla kennt nur Meeresspiegel 63. In den Epochen liegt das Meer woanders (Eiszeit 44, Kreide 80),
     * und Fische, Tintenfische und Delfine sollen dort spawnen, wo das Wasser wirklich ist.
     */
    @ModifyReturnValue(method = "getSeaLevel", at = @At("RETURN"))
    private int timemod$eraSeaLevel(int seaLevel) {
        if ((Object) this instanceof ServerLevel level && Eras.isEraDimension(level.dimension().location())) {
            return level.getChunkSource().getGenerator().getSeaLevel();
        }
        return seaLevel;
    }

    @WrapOperation(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void timemod$tickBlockEntity(TickingBlockEntity ticker, Operation<Void> original) {
        TimeEngine engine = timemod$engine;
        if (engine instanceof ServerTimeEngine server && server.journal().isRecording()) {
            // Tickende Block-Entities (Öfen, Trichter …) ändern sich oft ohne setChanged()
            server.journal().onBlockEntityChanged(ticker.getPos());
        }
        if (engine == null || engine.isIdle()) {
            original.call(ticker);
        } else {
            engine.tickBlockEntity(ticker, () -> original.call(ticker));
        }
    }
}
