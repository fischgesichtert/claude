package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Stasis-Zeiger eines Zeitwächters: ein leuchtender Minutenzeiger, der geradeaus fliegt. Wen er
 * trifft, dessen Zeit gerät ins Stocken – Wesen laufen drei Sekunden in Zeitlupe, Spieler (die
 * außerhalb der Zeit stehen) werden stattdessen schwer und langsam.
 */
public class StasisBolt extends AbstractHurtingProjectile implements ItemSupplier {
    public static final float DAMAGE = 4.0F;
    public static final int SLOW_TICKS = 60;
    public static final double SLOW_FACTOR = 0.35;
    private static final int LIFETIME = 70;

    public StasisBolt(EntityType<? extends StasisBolt> type, Level level) {
        super(type, level);
    }

    public StasisBolt(Level level, LivingEntity owner, Vec3 direction) {
        super(ModEntities.STASIS_BOLT, owner, direction.x, direction.y, direction.z, level);
        Vec3 dir = direction.normalize();
        xPower = dir.x * 0.08;
        yPower = dir.y * 0.08;
        zPower = dir.z * 0.08;
        setDeltaMovement(dir.scale(0.7));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof TimeWarden) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            return;
        }
        target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), DAMAGE);
        if (target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1), owner);
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, SLOW_TICKS, 0), owner);
        } else if (target instanceof LivingEntity) {
            ServerTimeEngine.of(server).addEntityZone(target, SLOW_FACTOR, TimeFilter.ALL_ENTITIES, owner == null ? null : owner.getUUID(), SLOW_TICKS);
        }
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 16, 0.2, 0.2, 0.2, 0.08);
        playSound(ModSounds.WARDEN_BOLT_HIT, 1.0F, 1.0F);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            playSound(ModSounds.WARDEN_BOLT_HIT, 0.7F, 1.3F);
        }
        discard();
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    /** Eine Spur aus Licht, die noch eine Weile in der Luft hängt. */
    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.END_ROD;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.CLOCK_HAND_BLADE);
    }
}
