package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.registry.ModDamageTypes;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ruinenwächter – was von den Eisengolems verlassener Städte blieb: ein Gerüst aus Stein und rostigem
 * Eisen, überwachsen von Moos. Meist ruht er an seinem Posten wie eine Statue. Kommt jemand näher, öffnet
 * sich sein Auge und folgt ihm; wer ihm zu nahe tritt, eine Truhe der Ruine öffnet oder ihre Mauern
 * abbaut, den greift er an. Ohne Besucher kehrt er an seinen Posten zurück und schläft wieder ein.
 * <p>
 * Die Zeit hat ihm zugesetzt: In beschleunigter Zeit holt sie ihn ein – er zerfällt.
 */
public class RuinGuardian extends IronGolem {
    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(RuinGuardian.class, EntityDataSerializers.BOOLEAN);
    /** Ab dieser Nähe erwacht er. */
    public static final double WAKE_RANGE = 10.0;
    /** Wer näher kommt, wird angegriffen. */
    public static final double INTRUDER_RANGE = 4.0;
    /** So lange ohne Besucher, bis er an seinen Posten zurückkehrt und einschläft. */
    public static final int LONELY_TICKS = 200;
    /** Wie stark ihn beschleunigte Zeit altern lässt: Schaden je 20 eigener Ticks. */
    public static final float AGING_DAMAGE = 1.5F;

    @Nullable
    private BlockPos post;
    private int lonely;
    /** Nur Client: wie weit das Auge geöffnet ist (0..1). */
    public float eyeOpen;
    public float eyeOpenO;

    public RuinGuardian(EntityType<? extends RuinGuardian> type, Level level) {
        super(type, level);
        xpReward = 20;
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 150.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_AWAKE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 24.0F));
        goalSelector.addGoal(4, new ReturnToPostGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, (float) WAKE_RANGE) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            player -> distanceToSqr(player) < INTRUDER_RANGE * INTRUDER_RANGE));
        targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    // ------------------------------------------------------------------ Posten und Schlaf

    public boolean isAwake() {
        return entityData.get(DATA_AWAKE);
    }

    @Nullable
    public BlockPos post() {
        return post;
    }

    /** Der Platz, den er bewacht (und an den er zurückkehrt). */
    public void setPost(BlockPos pos) {
        post = pos.immutable();
        restrictTo(post, 16);
    }

    public void wake() {
        if (!isAwake()) {
            entityData.set(DATA_AWAKE, true);
            playSound(ModSounds.GUARDIAN_WAKE, 1.5F, 0.9F + random.nextFloat() * 0.2F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                    getX(), getY(0.6), getZ(), 30, 0.5, 0.8, 0.5, 0.1);
            }
        }
        lonely = 0;
    }

    public void sleep() {
        entityData.set(DATA_AWAKE, false);
        getNavigation().stop();
        setTarget(null);
    }

    /** Ein Eindringling hat die Ruine angetastet: Der Wächter erwacht und nimmt ihn ins Visier. */
    public void alarm(Player player) {
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        wake();
        setPersistentAngerTarget(player.getUUID());
        startPersistentAngerTimer();
        setTarget(player);
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || (!isAwake() && !level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            eyeOpenO = eyeOpen;
            eyeOpen += ((isAwake() ? 1.0F : 0.0F) - eyeOpen) * 0.15F;
            return;
        }
        if (post == null) {
            setPost(blockPosition());
        }
        if (tickCount % 10 == 0) {
            Player near = level().getNearestPlayer(getX(), getY(), getZ(), WAKE_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            if (near != null || getTarget() != null) {
                wake();
            } else if (isAwake()) {
                lonely += 10;
                if (lonely >= LONELY_TICKS && getTarget() == null && post.closerToCenterThan(position(), 2.5)) {
                    sleep();
                }
            }
        }
        // Beschleunigte Zeit lässt ihn zerfallen: seine eigenen Ticks laufen dort schneller
        if (tickCount % 20 == 0) {
            TimeEngine engine = ((TimeLevel) level()).timemod$getEngine();
            TimeZone zone = engine == null || engine.isIdle() ? null : engine.resolveEntity(this);
            if (zone != null && !zone.isRewind() && zone.factor() > 1.0) {
                hurt(ModDamageTypes.oldAge(level()), AGING_DAMAGE);
                ((ServerLevel) level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                    getX(), getY(0.8), getZ(), 12, 0.5, 0.6, 0.5, 0.05);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide) {
            wake();
        }
        return hit;
    }

    /** Kein Eisenbarren repariert, was die Jahrhunderte geformt haben. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ Klang

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + random.nextFloat() * 0.1F;
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
        if (post != null) {
            tag.put("Post", NbtUtils.writeBlockPos(post));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_AWAKE, tag.getBoolean("Awake"));
        if (tag.contains("Post")) {
            setPost(NbtUtils.readBlockPos(tag.getCompound("Post")));
        }
    }

    /** Ohne Ziel und Besucher geht er langsam an seinen Posten zurück. */
    private final class ReturnToPostGoal extends Goal {
        ReturnToPostGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isAwake() && getTarget() == null && post != null && lonely > 60 && !post.closerToCenterThan(position(), 2.5);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.6);
        }
    }
}
