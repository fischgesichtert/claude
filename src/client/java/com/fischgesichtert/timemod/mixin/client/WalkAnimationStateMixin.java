package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.WalkAnimationAccess;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Beinbewegung nach mehreren Teilschritten über den ganzen Tick verteilen (statt zu springen). */
@Mixin(WalkAnimationState.class)
public abstract class WalkAnimationStateMixin implements WalkAnimationAccess {
    @Shadow
    private float speedOld;
    @Shadow
    private float position;

    @Unique
    private float timemod$renderDelta = Float.NaN;

    @Override
    public float timemod$speedOld() {
        return speedOld;
    }

    @Override
    public void timemod$setSpeedOld(float value) {
        speedOld = value;
    }

    @Override
    public void timemod$setRenderDelta(float delta) {
        timemod$renderDelta = delta;
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void timemod$resetDelta(float target, float smoothing, CallbackInfo ci) {
        timemod$renderDelta = Float.NaN;
    }

    @Inject(method = "position(F)F", at = @At("HEAD"), cancellable = true)
    private void timemod$smoothPosition(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!Float.isNaN(timemod$renderDelta)) {
            cir.setReturnValue(position - timemod$renderDelta * (1.0F - partialTick));
        }
    }
}
