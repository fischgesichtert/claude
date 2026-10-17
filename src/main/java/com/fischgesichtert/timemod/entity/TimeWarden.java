package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import com.fischgesichtert.timemod.paradox.TimeWardens;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeImmunity;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeitwächter – die Polizei der Zeitlinie. Sie stehen außerhalb der Zeit: Stopp, Zeitlupe, Raffer
 * und Zurückspulen gehen durch sie hindurch (Tag {@code #timemod:time_immune}). Ein Wächter jagt
 * einen bestimmten Spieler, springt ihm nach, wirft Stasis-Zeiger und setzt ihn mit einem Treffer
 * kurz fest – den Zeitarrest, vor dem nur die volle Chrono-Rüstung schützt.
 * <p>
 * Jeder Treffer treibt einen Teil der Schuld ein: Der Paradox-Druck des Gejagten sinkt um 3 %, und
 * etwas Chronon wird eingezogen. Fällt der Druck unter 25 %, ist der Fall erledigt – der Wächter
 * zieht sich zu einem Lichtstrich zusammen und verschwindet. Wer ihn besiegt, beruhigt die Zeitlinie
 * um 10 %. Wächter aus dem Spawn-Ei jagen ohne Fall einfach den nächsten Spieler.
 */
public class TimeWarden extends Monster {
    private static final EntityDataAccessor<Integer> DATA_ARRIVE = SynchedEntityData.defineId(TimeWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VANISH = SynchedEntityData.defineId(TimeWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAST = SynchedEntityData.defineId(TimeWarden.class, EntityDataSerializers.INT);
    /** Erscheinen und Verschwinden dauern so lange (Ticks). */
    public static final int FADE_TICKS = 16;
    public static final int CAST_TICKS = 12;
    /** So lange hält der Zeitarrest (Ticks). */
    public static final int ARREST_TICKS = 30;
    public static final int ARREST_COOLDOWN = 200;
    public static final int BLINK_COOLDOWN = 100;
    /** Paradox-Druck, den ein Treffer beim Gejagten eintreibt. */
    public static final float DEBT_PER_HIT = 3.0F;
    /** Chronon, das ein Treffer einzieht. */
    public static final int FINE = 200;
    /** Wer einen Wächter besiegt, beruhigt die Zeitlinie. */
    public static final float DEFEAT_RELIEF = 10.0F;

    @Nullable
    private UUID quarry;
    private int blinkCooldown = 40;
    private int boltCooldown = 60;
    private int arrestCooldown;
    private int stuckTicks;
    /** Nur Client: Drehung des Heiligenscheins aus Ziffern. */
    public float haloSpin;
    public float haloSpinO;

    public TimeWarden(EntityType<? extends TimeWarden> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ARRIVE, 0);
        entityData.define(DATA_VANISH, 0);
        entityData.define(DATA_CAST, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BoltGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Den Gejagten finden sie auch hinter Wänden
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
            player -> quarry == null || player.getUUID().equals(quarry)));
    }

    // ------------------------------------------------------------------ Der Fall

    /** Wen dieser Wächter jagt (oder null bei Wächtern ohne Fall). */
    @Nullable
    public UUID quarry() {
        return quarry;
    }

    public void hunt(ServerPlayer player) {
        quarry = player.getUUID();
        setTarget(player);
    }

    @Nullable
    private ServerPlayer quarryPlayer() {
        return quarry == null || getServer() == null ? null : getServer().getPlayerList().getPlayer(quarry);
    }

    /** Ist der Fall erledigt? Dann verschwindet der Wächter. */
    private void reviewCase() {
        if (quarry == null) {
            return;
        }
        ServerPlayer player = quarryPlayer();
        if (player == null || !player.isAlive() || player.level() != level() || !ParadoxPressure.counts(player)
            || ParadoxPressure.of(player) < ParadoxPressure.UNEASE) {
            vanish();
            return;
        }
        if (getTarget() == null || !getTarget().isAlive()) {
            setTarget(player);
        }
    }

    public boolean isFading() {
        return entityData.get(DATA_ARRIVE) > 0 || entityData.get(DATA_VANISH) > 0;
    }

    /** 0..1: wie weit der Wächter gerade da ist (für die Darstellung als zusammengezogener Lichtstrich). */
    public float presence(float partialTick) {
        int arrive = entityData.get(DATA_ARRIVE);
        if (arrive > 0) {
            return Mth.clamp(1.0F - (arrive - partialTick) / FADE_TICKS, 0.0F, 1.0F);
        }
        int vanish = entityData.get(DATA_VANISH);
        if (vanish > 0) {
            return Mth.clamp((vanish - partialTick) / FADE_TICKS, 0.0F, 1.0F);
        }
        return 1.0F;
    }

    public int castTicks() {
        return entityData.get(DATA_CAST);
    }

    /** Der Auftritt: ein Riss im Licht, aus dem sich der Wächter entfaltet. */
    public void arrive() {
        entityData.set(DATA_ARRIVE, FADE_TICKS);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 60, 0.3, 1.2, 0.3, 0.12);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 24, 0.1, 1.0, 0.1, 0.02);
        }
        playSound(ModSounds.WARDEN_APPEAR, 2.0F, 1.0F);
    }

    /** Fall erledigt: Der Wächter zieht sich zu einem Strich zusammen und ist fort. */
    public void vanish() {
        if (entityData.get(DATA_VANISH) > 0) {
            return;
        }
        entityData.set(DATA_ARRIVE, 0);
        entityData.set(DATA_VANISH, FADE_TICKS);
        getNavigation().stop();
        setTarget(null);
        playSound(ModSounds.WARDEN_VANISH, 1.6F, 1.0F);
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            haloSpinO = haloSpin;
            haloSpin += castTicks() > 0 ? 0.35F : 0.04F;
            if (random.nextInt(4) == 0 && !isFading()) {
                Vec3 behind = position().add(0, getBbHeight() * 0.86, 0).subtract(getLookAngle().multiply(1, 0, 1).scale(0.35));
                level().addParticle(ParticleTypes.END_ROD, behind.x + (random.nextDouble() - 0.5) * 0.6, behind.y + (random.nextDouble() - 0.5) * 0.6,
                    behind.z + (random.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
            }
            return;
        }
        int arrive = entityData.get(DATA_ARRIVE);
        if (arrive > 0) {
            entityData.set(DATA_ARRIVE, arrive - 1);
        }
        int vanish = entityData.get(DATA_VANISH);
        if (vanish > 0) {
            if (vanish == 1) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 30, 0.05, 1.0, 0.05, 0.05);
                discard();
                return;
            }
            entityData.set(DATA_VANISH, vanish - 1);
            return;
        }
        if (entityData.get(DATA_CAST) > 0) {
            entityData.set(DATA_CAST, entityData.get(DATA_CAST) - 1);
        }
        blinkCooldown = Math.max(0, blinkCooldown - 1);
        boltCooldown = Math.max(0, boltCooldown - 1);
        arrestCooldown = Math.max(0, arrestCooldown - 1);
        if (tickCount % 20 == 0) {
            reviewCase();
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            stuckTicks = 0;
            return;
        }
        double distance = distanceTo(target);
        stuckTicks = getNavigation().isDone() && distance > 3.0 ? stuckTicks + 1 : 0;
        boolean above = target.getY() - getY() > 3.0 && distance < 12.0;
        if (blinkCooldown == 0 && (distance > 14.0 || stuckTicks > 30 || above)) {
            blinkTo(target);
        }
    }

    /** Ein Sprung durch die Zeit hinter das Ziel (2–4 Blöcke, entgegen seiner Blickrichtung). */
    public boolean blinkTo(LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        for (int attempt = 0; attempt < 12; attempt++) {
            double back = 2.0 + random.nextDouble() * 2.0;
            double side = (random.nextDouble() - 0.5) * 3.0;
            Vec3 at = target.position().subtract(look.scale(back)).add(-look.z * side, 0, look.x * side);
            BlockPos feet = BlockPos.containing(at);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = feet.above(dy);
                if (canStand(pos)) {
                    jump(Vec3.atBottomCenterOf(pos));
                    getLookControl().setLookAt(target, 180.0F, 180.0F);
                    blinkCooldown = BLINK_COOLDOWN + random.nextInt(40);
                    stuckTicks = 0;
                    return true;
                }
            }
        }
        blinkCooldown = 20;
        return false;
    }

    /** Ausweichen: ein kurzer Sprung zur Seite. */
    private boolean sidestep() {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            BlockPos feet = BlockPos.containing(getX() + Math.cos(angle) * 3.0, getY(), getZ() + Math.sin(angle) * 3.0);
            for (int dy = 1; dy >= -2; dy--) {
                if (canStand(feet.above(dy))) {
                    jump(Vec3.atBottomCenterOf(feet.above(dy)));
                    blinkCooldown = Math.max(blinkCooldown, 40);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canStand(BlockPos feet) {
        if (!level().getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockState ground = level().getBlockState(feet.below());
        if (!ground.isFaceSturdy(level(), feet.below(), Direction.UP)) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            BlockState state = level().getBlockState(feet.above(i));
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level(), feet.above(i)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void jump(Vec3 to) {
        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 30, 0.25, 0.9, 0.25, 0.06);
        playSound(ModSounds.WARDEN_BLINK, 1.2F, 1.0F);
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 14, 0.15, 0.9, 0.15, 0.02);
        playSound(ModSounds.WARDEN_BLINK, 1.2F, 1.3F);
    }

    // ------------------------------------------------------------------ Kampf

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player && player.getUUID().equals(quarry)) {
            // Die Schuld wird eingetrieben: weniger Druck, etwas Chronon weniger
            ParadoxPressure.relieve(player, DEBT_PER_HIT);
            Chronon.drain(player, FINE);
            if (arrestCooldown == 0 && !TimeImmunity.isImmune(player) && player.isAlive()) {
                arrest(player);
            }
        }
        return hit;
    }

    /** Zeitarrest: Der Gejagte steht anderthalb Sekunden still – Treffer darin kommen beim Loslassen auf einmal. */
    public void arrest(ServerPlayer player) {
        ServerTimeEngine.of((ServerLevel) level()).addEntityZone(player, 0.0, TimeFilter.PLAYERS, getUUID(), ARREST_TICKS);
        playSound(ModSounds.WARDEN_ARREST, 1.6F, 1.0F);
        player.displayClientMessage(Component.translatable("message.timemod.warden.arrest").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        arrestCooldown = ARREST_COOLDOWN;
    }

    /** Wirft sofort einen Stasis-Zeiger auf {@code target} (mit Wurfbewegung) – für Vorführungen und Befehle. */
    public void throwBolt(LivingEntity target) {
        entityData.set(DATA_CAST, CAST_TICKS);
        shoot(target);
    }

    private void shoot(LivingEntity target) {
        Vec3 from = new Vec3(getX(), getY(0.72), getZ());
        Vec3 aim = target.getEyePosition().subtract(from);
        StasisBolt bolt = new StasisBolt(level(), this, aim);
        Vec3 start = from.add(aim.normalize().scale(0.8));
        bolt.setPos(start.x, start.y, start.z);
        level().addFreshEntity(bolt);
        playSound(ModSounds.WARDEN_BOLT, 1.5F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFading() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // Pfeilen weichen sie oft mit einem kurzen Zeitsprung aus
        if (!level().isClientSide && source.getDirectEntity() instanceof Projectile && random.nextFloat() < 0.35F && sidestep()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 40, 0.3, 0.8, 0.3, 0.08);
            if (source.getEntity() instanceof ServerPlayer player) {
                ParadoxPressure.relieve(player, DEFEAT_RELIEF);
                TimeWardens.onDefeated(player);
            }
        }
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isFading();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ Klang

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WARDEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHAIN_STEP, 0.25F, 0.8F);
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (quarry != null) {
            tag.putUUID("Quarry", quarry);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
    }

    /** Fernkampf: alle paar Sekunden ein Stasis-Zeiger, wenn das Ziel sichtbar und nicht zu nah ist. */
    private final class BoltGoal extends Goal {
        private int windup;

        BoltGoal() {
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || boltCooldown > 0 || isFading()) {
                return false;
            }
            double distance = distanceTo(target);
            return distance >= 5.0 && distance <= 24.0 && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            windup = CAST_TICKS;
            entityData.set(DATA_CAST, CAST_TICKS);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--windup == 3 && hasLineOfSight(target)) {
                    shoot(target);
                }
            }
        }

        @Override
        public void stop() {
            windup = 0;
            boltCooldown = 50 + random.nextInt(40);
        }
    }
}
