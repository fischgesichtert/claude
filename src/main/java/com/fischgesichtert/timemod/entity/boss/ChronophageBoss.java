package com.fischgesichtert.timemod.entity.boss;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.entity.TimeLeech;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeHitQueue;
import com.fischgesichtert.timemod.time.TimeLevel;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Der Chronophage – ein Zeitfresser. Er verlangsamt alles um sich, saugt Chronon aus Werkzeugen
 * und spult seine eigenen Wunden zurück. Sein Maul ist meist geschlossen (fast unverwundbar); nach
 * einer Vorwarnung öffnet es sich kurz. Wer genau dann die Zeit anhält, kann ihn treffen – die im
 * Stillstand gesammelten Treffer schlagen ein, und diese Wunden kann er nicht zurückspulen.
 */
public class ChronophageBoss extends Monster {
    public enum Mouth {
        CLOSED, OPENING, OPEN
    }

    private static final EntityDataAccessor<Integer> DATA_MOUTH = SynchedEntityData.defineId(ChronophageBoss.class, EntityDataSerializers.INT);
    private static final DustParticleOptions DRAIN = new DustParticleOptions(new Vector3f(0.8F, 0.3F, 1.0F), 1.2F);
    public static final int OPENING_TICKS = 30;
    public static final float CLOSED_FACTOR = 0.1F;
    public static final int DRAIN_CHRONON = 250;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int mouthTimer = closedTicks();
    /** Schaden, den er beim nächsten Schließen zurückspult. */
    private float rewindable;
    private int leechCooldown = 80;
    private int drainCooldown = 140;
    private boolean secondPhase;
    /** Nur Client: Maulöffnung (0..1), für weiche Animation. */
    public float mouthOpen;
    public float mouthOpenO;

    public ChronophageBoss(EntityType<? extends ChronophageBoss> type, Level level) {
        super(type, level);
        xpReward = 80;
        moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 400.0)
            .add(Attributes.FLYING_SPEED, 0.45)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MOUTH, Mouth.CLOSED.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new HoverAndBiteGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public Mouth mouth() {
        return Mouth.values()[entityData.get(DATA_MOUTH)];
    }

    public void setMouth(Mouth mouth, int ticks) {
        entityData.set(DATA_MOUTH, mouth.ordinal());
        mouthTimer = ticks;
    }

    private int closedTicks() {
        return secondPhase ? 100 : 130;
    }

    private int openTicks() {
        return secondPhase ? 24 : 30;
    }

    public float rewindable() {
        return rewindable;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            mouthOpenO = mouthOpen;
            float want = switch (mouth()) {
                case CLOSED -> 0.0F;
                case OPENING -> 0.35F;
                case OPEN -> 1.0F;
            };
            mouthOpen = net.minecraft.util.Mth.approach(mouthOpen, want, 0.08F);
            if (mouth() != Mouth.CLOSED && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.8), getY(0.5), getRandomZ(0.8), 0, 0.05, 0);
            }
            return;
        }
        // Maul-Zyklus: geschlossen → Vorwarnung → offen → geschlossen (spult dabei die Wunden zurück)
        if (--mouthTimer <= 0) {
            switch (mouth()) {
                case CLOSED -> {
                    setMouth(Mouth.OPENING, OPENING_TICKS);
                    playSound(ModSounds.PHAGE_OPEN, 3.0F, 1.0F);
                }
                case OPENING -> setMouth(Mouth.OPEN, openTicks());
                case OPEN -> {
                    setMouth(Mouth.CLOSED, closedTicks());
                    rewindWounds();
                }
            }
        }
        // Aura: alles in der Nähe wird langsam
        if (tickCount % 20 == 0) {
            for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(14.0), p -> !p.isCreative() && !p.isSpectator())) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false));
            }
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            if (--leechCooldown <= 0 && hasLineOfSight(target)) {
                shootLeeches(target);
                leechCooldown = secondPhase ? 50 : 70;
            }
            if (--drainCooldown <= 0 && mouth() == Mouth.CLOSED && target instanceof Player player && distanceToSqr(player) < 20 * 20) {
                drainTime(player);
                drainCooldown = secondPhase ? 120 : 160;
            }
        }
        if (!secondPhase && getHealth() < getMaxHealth() / 2) {
            secondPhase = true;
            playSound(ModSounds.PHAGE_ROAR, 4.0F, 0.6F);
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) {
            return super.hurt(source, amount);
        }
        TimeEngine engine = ((TimeLevel) level()).timemod$getEngine();
        if (engine != null && !engine.isIdle() && engine.isFrozen(this)) {
            // Im Stillstand wird nur gesammelt – gewertet wird beim Weiterlaufen
            return super.hurt(source, amount);
        }
        // Treffer aus dem Stillstand, während das Maul offen ist: voller Schaden, nicht rückspulbar
        boolean frozenBurst = ((TimeHitQueue) this).timemod$isReleasingHits() && mouth() == Mouth.OPEN;
        float scaled = mouth() == Mouth.OPEN ? amount : amount * CLOSED_FACTOR;
        float before = getHealth();
        boolean hit = super.hurt(source, scaled);
        if (hit && !frozenBurst) {
            rewindable += before - getHealth();
        }
        if (hit && frozenBurst && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(0.5), getZ(), 20, 0.8, 0.8, 0.8, 0.3);
        }
        return hit;
    }

    /** Spult alle gewöhnlichen Wunden seit dem letzten Mal zurück. */
    public void rewindWounds() {
        if (rewindable <= 0 || !isAlive()) {
            rewindable = 0;
            return;
        }
        setHealth(Math.min(getMaxHealth(), getHealth() + rewindable));
        rewindable = 0;
        playSound(ModSounds.REWIND, 2.5F, 0.8F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 80, 1.0, 1.0, 1.0, 0.2);
        }
    }

    private void shootLeeches(LivingEntity target) {
        int count = secondPhase ? 4 : 3;
        for (int i = 0; i < count; i++) {
            TimeLeech leech = new TimeLeech(level(), this, target);
            double a = 2 * Math.PI * i / count + random.nextDouble();
            leech.setPos(getX() + Math.cos(a) * 1.2, getY(0.5), getZ() + Math.sin(a) * 1.2);
            level().addFreshEntity(leech);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
    }

    /** Frisst Chronon aus dem Inventar des Spielers und heilt sich damit. */
    private void drainTime(Player player) {
        int drained = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ChrononHolder && drained < DRAIN_CHRONON) {
                drained += Chronon.extract(stack, DRAIN_CHRONON - drained);
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
        heal(4.0F + drained / 25.0F);
        playSound(ModSounds.PHAGE_DRAIN, 2.5F, 1.0F);
        if (level() instanceof ServerLevel server) {
            Vec3 from = player.getEyePosition();
            Vec3 to = position().add(0, getBbHeight() / 2, 0);
            for (int i = 0; i <= 20; i++) {
                Vec3 p = from.lerp(to, i / 20.0);
                server.sendParticles(DRAIN, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PHAGE_ROAR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return mouth() == Mouth.OPEN ? SoundEvents.WARDEN_HURT : SoundEvents.SHULKER_HURT_CLOSED;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PHAGE_ROAR;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
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
        tag.putFloat("Rewindable", rewindable);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        secondPhase = tag.getBoolean("SecondPhase");
        rewindable = tag.getFloat("Rewindable");
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    /** Schwebt in Bissweite um sein Ziel, stößt zu, wenn es nah genug ist. */
    private final class HoverAndBiteGoal extends Goal {
        private int biteCooldown;

        HoverAndBiteGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            if (biteCooldown > 0) {
                biteCooldown--;
            }
            if (distance < 4.0 && biteCooldown == 0) {
                doHurtTarget(target);
                biteCooldown = 30;
                playSound(ModSounds.PHAGE_BITE, 2.0F, 1.0F);
            }
            // Umkreisen in 5–7 Blöcken Abstand, leicht über dem Ziel
            double angle = (tickCount / 60.0) + getId();
            Vec3 orbit = target.position().add(Math.cos(angle) * 6.0, 2.5 + Math.sin(tickCount / 25.0), Math.sin(angle) * 6.0);
            Vec3 goal = distance > 100 ? target.position().add(0, 2, 0) : orbit;
            if (mouth() == Mouth.CLOSED && tickCount % 80 < 15) {
                goal = target.getEyePosition();
            }
            getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.0);
        }
    }
}
