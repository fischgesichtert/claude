package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.safety.CrashGuard;
import com.fischgesichtert.timemod.time.TimeContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Absturzschutz für Entity-Ticks (Server und Client): Ein Fehler aus TIME macht hier keinen Absturz,
 * sondern lässt nur diesen einen Tick ausfallen. Alles andere behandelt Minecraft wie gewohnt.
 */
@Mixin(Level.class)
abstract class CrashGuardLevelMixin {
    @WrapOperation(method = "guardEntityTick", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void timemod$guardEntityTick(Consumer<Object> tick, Object entity, Operation<Void> original) {
        try {
            original.call(tick, entity);
        } catch (Throwable error) {
            // Der Tick ist mittendrin abgebrochen: keine Eigenzeit darf danach hängen bleiben
            TimeContext.exit();
            if (!(entity instanceof Entity scope) || !CrashGuard.absorbEntity(scope, error)) {
                throw error;
            }
        }
    }
}
