package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Zahnrad-Säge des Uhrmachers: fliegt geradeaus, kreischt, schneidet. */
public class GearSaw extends AbstractHurtingProjectile implements ItemSupplier {
    public static final float DAMAGE = 7.0F;
    private static final int LIFETIME = 80;

    public GearSaw(EntityType<? extends GearSaw> type, Level level) {
        super(type, level);
    }

    public GearSaw(Level level, LivingEntity owner, Vec3 direction) {
        super(ModEntities.GEAR_SAW, owner, direction.x, direction.y, direction.z, level);
        Vec3 dir = direction.normalize();
        xPower = dir.x * 0.12;
        yPower = dir.y * 0.12;
        zPower = dir.z * 0.12;
        setDeltaMovement(dir.scale(0.6));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            return;
        }
        target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), DAMAGE);
        playSound(SoundEvents.ANVIL_LAND, 0.5F, 2.0F);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.2);
            playSound(SoundEvents.CHAIN_BREAK, 0.8F, 1.4F);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.CRIT;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.BRASS_GEAR);
    }
}
