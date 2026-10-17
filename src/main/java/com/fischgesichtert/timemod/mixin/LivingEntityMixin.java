package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeHitQueue;
import com.fischgesichtert.timemod.time.TimeLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements TimeHitQueue {
    @Unique
    private List<Hit> timemod$hits;
    @Unique
    private boolean timemod$releasing;

    @Unique
    private record Hit(DamageSource source, float amount) {
    }

    @Override
    public boolean timemod$hasPendingHits() {
        return timemod$hits != null && !timemod$hits.isEmpty();
    }

    @Override
    public void timemod$releaseHits() {
        if (!timemod$hasPendingHits()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        List<Hit> hits = timemod$hits;
        timemod$hits = null;
        timemod$releasing = true;
        try {
            for (Hit hit : hits) {
                if (self.isDeadOrDying()) {
                    break;
                }
                self.invulnerableTime = 0;
                self.hurt(hit.source(), hit.amount());
            }
        } finally {
            timemod$releasing = false;
        }
    }

    /** Im Stillstand kommt ein Treffer an, wirkt aber erst, wenn die Zeit weiterläuft. */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void timemod$queueHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (timemod$releasing || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        TimeEngine engine = ((TimeLevel) level).timemod$getEngine();
        if (engine == null || engine.isIdle() || !engine.isFrozen(self)) {
            return;
        }
        if (self.isDeadOrDying() || self.isInvulnerableTo(source) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (timemod$hits == null) {
            timemod$hits = new ArrayList<>();
        }
        timemod$hits.add(new Hit(source, amount));
        level.sendParticles(ParticleTypes.CRIT, self.getX(), self.getY(0.6), self.getZ(), 8, 0.25, 0.3, 0.25, 0.0);
        cir.setReturnValue(true);
    }
}
