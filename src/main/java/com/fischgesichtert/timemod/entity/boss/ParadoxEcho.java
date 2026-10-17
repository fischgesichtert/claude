package com.fischgesichtert.timemod.entity.boss;

import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Echo von Paradox: durchscheinend, schnell und zerbrechlich. Es lebt nur eine halbe Minute
 * und zerspringt beim ersten richtigen Treffer.
 */
public class ParadoxEcho extends Monster {
    private static final EntityDataAccessor<Optional<UUID>> DATA_ECHO_OF = SynchedEntityData.defineId(ParadoxEcho.class,
        EntityDataSerializers.OPTIONAL_UUID);
    public static final int LIFETIME = 600;
    private int life = LIFETIME;

    public ParadoxEcho(EntityType<? extends ParadoxEcho> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.36)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ECHO_OF, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Übernimmt Aussehen und Waffe von Paradox. */
    public void copyFrom(ParadoxBoss boss) {
        entityData.set(DATA_ECHO_OF, Optional.ofNullable(boss.echoOf()));
        setItemSlot(EquipmentSlot.MAINHAND, boss.getMainHandItem().copy());
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Nullable
    public UUID echoOf() {
        return entityData.get(DATA_ECHO_OF).orElse(null);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && --life <= 0) {
            shatter();
        }
    }

    /** Zerspringt in Zeitsplitter (ohne Beute). */
    public void shatter() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.3, 0.7, 0.3, 0.12);
            playSound(ModSounds.ECHO_SHATTER, 1.2F, 0.9F + random.nextFloat() * 0.3F);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        shatter();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ECHO_SHATTER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ECHO_SHATTER;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
