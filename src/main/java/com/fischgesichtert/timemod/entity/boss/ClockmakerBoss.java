package com.fischgesichtert.timemod.entity.boss;

import com.fischgesichtert.timemod.entity.GearSaw;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.StopResistant;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Der Uhrmacher – ein Uhrwerk-Titan. Getroffen springt er drei Sekunden in der Zeit zurück und
 * macht damit den Schaden ungeschehen; seine Panzerung schluckt die Hälfte jedes Treffers. Nur in
 * Zeitlupe öffnet sich seine Brustklappe: Dann trifft man das Uhrwerk dahinter doppelt, und er kann
 * nicht mehr zurückspringen. Anhalten lässt er sich nicht.
 */
public class ClockmakerBoss extends Monster implements StopResistant {
    private static final EntityDataAccessor<Boolean> DATA_SLOWED = SynchedEntityData.defineId(ClockmakerBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_THROW = SynchedEntityData.defineId(ClockmakerBoss.class, EntityDataSerializers.INT);
    /** Wie weit der Zeitsprung zurückreicht (Ticks). */
    public static final int JUMP_TICKS = 60;
    public static final int JUMP_COOLDOWN = 160;
    /** Ab so viel Schaden seit dem letzten Sprung springt er (wenn er kann). */
    public static final float JUMP_THRESHOLD = 16.0F;
    public static final float ARMOR_FACTOR = 0.5F;
    public static final float WEAK_SPOT_FACTOR = 2.0F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    private final float[] healthHistory = new float[JUMP_TICKS];
    private final Vec3[] positionHistory = new Vec3[JUMP_TICKS];
    private int historyIndex;
    private int historyCount;
    private int jumpCooldown;
    private float damageSinceJump;
    private int sawCooldown = 60;
    private boolean secondPhase;
    /** Nur Client: wie weit die Brustklappe offen ist (0..1), für weiche Animation. */
    public float doorOpen;
    public float doorOpenO;

    public ClockmakerBoss(EntityType<? extends ClockmakerBoss> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_SLOWED, false);
        entityData.define(DATA_THROW, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new GearSawGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Steht er gerade in Zeitlupe (0 < Faktor < 1)? Dann ist die Schwachstelle offen. */
    public boolean isWeakSpotExposed() {
        return entityData.get(DATA_SLOWED);
    }

    /** 0..1 – Wurfbewegung für die Darstellung. */
    public int throwTicks() {
        return entityData.get(DATA_THROW);
    }

    private boolean computeSlowed() {
        TimeEngine engine = ((TimeLevel) level()).timemod$getEngine();
        if (engine == null || engine.isIdle()) {
            return false;
        }
        TimeZone zone = engine.resolveEntity(this);
        return zone != null && !zone.isRewind() && zone.factor() > 0.0 && zone.factor() < 1.0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            doorOpenO = doorOpen;
            doorOpen = Mth.approach(doorOpen, isWeakSpotExposed() ? 1.0F : 0.0F, 0.12F);
            if (isWeakSpotExposed() && random.nextInt(3) == 0) {
                Vec3 chest = position().add(getLookAngle().scale(0.8)).add(0, getBbHeight() * 0.62, 0);
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, chest.x, chest.y, chest.z, 0, 0.05, 0);
            }
            return;
        }
        entityData.set(DATA_SLOWED, computeSlowed());
        if (entityData.get(DATA_THROW) > 0) {
            entityData.set(DATA_THROW, entityData.get(DATA_THROW) - 1);
        }
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        // Gedächtnis der letzten drei Sekunden
        healthHistory[historyIndex] = getHealth();
        positionHistory[historyIndex] = position();
        historyIndex = (historyIndex + 1) % JUMP_TICKS;
        historyCount = Math.min(JUMP_TICKS, historyCount + 1);

        if (!secondPhase && getHealth() < getMaxHealth() / 2) {
            secondPhase = true;
            playSound(ModSounds.CLOCKMAKER_ROAR, 3.0F, 0.7F);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        bossEvent.setColor(isWeakSpotExposed() ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.YELLOW);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) {
            return super.hurt(source, amount);
        }
        boolean exposed = isWeakSpotExposed() || computeSlowed();
        float scaled = amount * (exposed ? WEAK_SPOT_FACTOR : ARMOR_FACTOR);
        float before = getHealth();
        boolean hit = super.hurt(source, scaled);
        if (hit && isAlive()) {
            if (exposed) {
                playSound(SoundEvents.ANVIL_LAND, 0.6F, 1.8F);
            } else {
                damageSinceJump += before - getHealth();
                if (jumpCooldown == 0 && damageSinceJump >= JUMP_THRESHOLD) {
                    timeJump();
                }
            }
        }
        return hit;
    }

    /** Springt zum ältesten Stand im Gedächtnis zurück: Leben und Ort wie vor drei Sekunden. */
    public void timeJump() {
        if (historyCount == 0) {
            return;
        }
        int oldest = historyCount < JUMP_TICKS ? 0 : historyIndex;
        float health = healthHistory[oldest];
        Vec3 position = positionHistory[oldest];
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 60, 0.6, 1.2, 0.6, 0.15);
        setHealth(Math.max(getHealth(), health));
        teleportTo(position.x, position.y, position.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 60, 0.6, 1.2, 0.6, 0.15);
        playSound(ModSounds.REWIND, 2.0F, 0.6F);
        jumpCooldown = JUMP_COOLDOWN;
        damageSinceJump = 0.0F;
        historyCount = 0;
    }

    public int jumpCooldown() {
        return jumpCooldown;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(getLookAngle().scale(0.8)).add(0, 0.35, 0));
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.6F);
        }
        return hit;
    }

    private void throwSaws(LivingEntity target) {
        int count = secondPhase ? 5 : 3;
        double spread = 0.22;
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
        Vec3 aim = target.getEyePosition().subtract(from).normalize();
        for (int i = 0; i < count; i++) {
            double angle = (i - (count - 1) / 2.0) * spread;
            Vec3 dir = aim.yRot((float) angle);
            GearSaw saw = new GearSaw(level(), this, dir);
            saw.setPos(from.x + dir.x, from.y, from.z + dir.z);
            level().addFreshEntity(saw);
        }
        entityData.set(DATA_THROW, 10);
        playSound(ModSounds.GEAR_SAW, 2.0F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CLOCK_TICK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CLOCKMAKER_ROAR;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SecondPhase", secondPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        secondPhase = tag.getBoolean("SecondPhase");
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    /** Fernkampf: alle paar Sekunden ein Fächer aus Zahnrad-Sägen. */
    private final class GearSawGoal extends Goal {
        GearSawGoal() {
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            if (--sawCooldown <= 0 && hasLineOfSight(target) && distanceToSqr(target) > 9.0) {
                throwSaws(target);
                sawCooldown = secondPhase ? 40 : 60 + random.nextInt(20);
            }
            sawCooldown = Mth.clamp(sawCooldown, -1, 200);
        }
    }
}
