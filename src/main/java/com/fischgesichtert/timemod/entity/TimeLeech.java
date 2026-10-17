package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Zeitegel des Chronophagen: sucht sein Ziel, frisst Sekunden (Verlangsamung) und Leben. */
public class TimeLeech extends AbstractHurtingProjectile implements ItemSupplier {
    public static final float DAMAGE = 5.0F;
    private static final int LIFETIME = 120;
    @Nullable
    private LivingEntity target;

    public TimeLeech(EntityType<? extends TimeLeech> type, Level level) {
        super(type, level);
    }

    public TimeLeech(Level level, LivingEntity owner, LivingEntity target) {
        super(ModEntities.TIME_LEECH, owner, 0, 0, 0, level);
        this.target = target;
        setDeltaMovement(target.getEyePosition().subtract(owner.getEyePosition()).normalize().scale(0.35));
        xPower = yPower = zPower = 0;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (target != null && target.isAlive()) {
                // Sanft nachsteuern
                Vec3 want = target.getEyePosition().subtract(position()).normalize().scale(0.42);
                setDeltaMovement(getDeltaMovement().scale(0.9).add(want.scale(0.1)));
            }
            if (tickCount > LIFETIME) {
                discard();
            }
        }
        super.tick();
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity hit = result.getEntity();
        Entity owner = getOwner();
        if (hit == owner || hit instanceof TimeLeech) {
            return;
        }
        hit.hurt(damageSources().indirectMagic(this, owner), DAMAGE);
        if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.TIME_CORE_FRAGMENT);
    }
}
