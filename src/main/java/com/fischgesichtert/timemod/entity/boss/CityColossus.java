package com.fischgesichtert.timemod.entity.boss;

import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.registry.ModDamageTypes;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.StopResistant;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Der Wächter der letzten Stadt – Epochen-Boss von +1000. Ein Koloss aus hellem Kalkstein und grün
 * angelaufenem Kupfer, der Kopf ein Glockenturm, die rechte Hand eine Glocke, auf der Brust die Uhr der
 * Stadt. Er hat tausend Jahre überdauert: Er heilt sich ständig, und Schläge prallen zum Teil an ihm ab –
 * nur in <b>beschleunigter Zeit</b> holen ihn die Jahrhunderte ein (mehr Schaden, keine Heilung, er
 * zerfällt). Seine Glocke stellt die Zeit um ihn wieder richtig: Ihr Schlag löst alle Zeitzonen in der
 * Nähe auf und stößt Angreifer zurück.
 * <p>
 * Dazu sein Verwitterungshauch (er lässt Rüstungen altern), ein Stampfen gegen alle, die zu nah kommen, und
 * bei zwei Dritteln und einem Drittel seines Lebens ruft er Ruinenwächter aus dem Boden. Unter der Hälfte
 * fallen seine Kupferplatten ab: Er wird schneller, die Glocke schlägt öfter, und auf seinen Blitzableiter
 * folgt ein Gewitter über seinen Gegnern.
 */
public class CityColossus extends Monster implements StopResistant {
    private static final EntityDataAccessor<Integer> DATA_EMERGE = SynchedEntityData.defineId(CityColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRIKE = SynchedEntityData.defineId(CityColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STOMP = SynchedEntityData.defineId(CityColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BREATH = SynchedEntityData.defineId(CityColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BARE = SynchedEntityData.defineId(CityColossus.class, EntityDataSerializers.BOOLEAN);

    public static final int EMERGE_TICKS = 70;
    public static final int STRIKE_TICKS = 24;
    /** In diesem Tick der Bewegung schlägt die Glocke an (von STRIKE_TICKS abwärts gezählt). */
    public static final int STRIKE_HIT = 12;
    public static final int STOMP_TICKS = 18;
    public static final int STOMP_HIT = 9;
    public static final int BREATH_TICKS = 30;
    public static final double BELL_RADIUS = 10.0;
    public static final double BREATH_RANGE = 12.0;
    public static final double STOMP_RADIUS = 4.5;
    /** Schaden in beschleunigter Zeit (die Jahrhunderte holen ihn ein) und sonst (er hält stand). */
    public static final float FAST_FACTOR = 1.5F;
    public static final float ENDURING_FACTOR = 0.6F;
    /** Heilung pro Sekunde außerhalb beschleunigter Zeit. */
    public static final float REGEN = 1.0F;
    /** Zerfall pro 20 eigener Ticks in beschleunigter Zeit. */
    public static final float AGING = 2.0F;
    /** Wie stark der Hauch Rüstungen altern lässt (Anteil der Haltbarkeit je Teil). */
    public static final float WEATHERING = 0.08F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
    private int bellCooldown = 120;
    private int breathCooldown = 180;
    private int stompCooldown = 60;
    private int stormCooldown = 160;
    private boolean calledAtTwoThirds;
    private boolean calledAtOneThird;
    private final List<Strike> strikes = new ArrayList<>();

    /** Ein angekündigter Blitz: Ort und verbleibende Ticks. */
    private static final class Strike {
        final Vec3 at;
        int ticks;

        Strike(Vec3 at, int ticks) {
            this.at = at;
            this.ticks = ticks;
        }
    }

    public CityColossus(EntityType<? extends CityColossus> type, Level level) {
        super(type, level);
        xpReward = 150;
        setPersistenceRequired();
        setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 600.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.ATTACK_DAMAGE, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_EMERGE, 0);
        entityData.define(DATA_STRIKE, 0);
        entityData.define(DATA_STOMP, 0);
        entityData.define(DATA_BREATH, 0);
        entityData.define(DATA_BARE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ------------------------------------------------------------------ Zustand für die Darstellung

    public int emergeTicks() {
        return entityData.get(DATA_EMERGE);
    }

    public int strikeTicks() {
        return entityData.get(DATA_STRIKE);
    }

    public int stompTicks() {
        return entityData.get(DATA_STOMP);
    }

    public int breathTicks() {
        return entityData.get(DATA_BREATH);
    }

    /** Zweite Phase: Die Kupferplatten sind abgefallen. */
    public boolean isBare() {
        return entityData.get(DATA_BARE);
    }

    /** Er steigt aus dem Boden (beim Erwecken an der Glocke). */
    public void emerge() {
        entityData.set(DATA_EMERGE, EMERGE_TICKS);
        playSound(ModSounds.COLOSSUS_EMERGE, 4.0F, 1.0F);
    }

    private boolean busy() {
        return emergeTicks() > 0 || strikeTicks() > 0 || stompTicks() > 0 || breathTicks() > 0;
    }

    /** Steht er gerade in beschleunigter Zeit? Dann holen ihn die Jahrhunderte ein. */
    public boolean isAging() {
        TimeEngine engine = ((TimeLevel) level()).timemod$getEngine();
        if (engine == null || engine.isIdle()) {
            return false;
        }
        TimeZone zone = engine.resolveEntity(this);
        return zone != null && !zone.isRewind() && zone.factor() > 1.0;
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || emergeTicks() > 0;
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (breathTicks() > 0) {
                Vec3 look = getLookAngle();
                Vec3 mouth = position().add(0, getBbHeight() * 0.8, 0).add(look.scale(1.2));
                for (int i = 0; i < 4; i++) {
                    Vec3 v = look.add((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.6) * 0.3, (random.nextDouble() - 0.5) * 0.5).scale(0.5);
                    level().addParticle(i % 2 == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
                }
            }
            return;
        }
        ServerLevel server = (ServerLevel) level();
        int emerge = emergeTicks();
        if (emerge > 0) {
            entityData.set(DATA_EMERGE, emerge - 1);
            if (emerge % 4 == 0) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), getX(), getY() + 0.2, getZ(),
                    20, 1.6, 0.2, 1.6, 0.1);
            }
            bossEvent.setProgress(getHealth() / getMaxHealth());
            return;
        }
        countDown(DATA_STRIKE);
        countDown(DATA_STOMP);
        countDown(DATA_BREATH);
        boolean aging = isAging();
        if (tickCount % 20 == 0) {
            if (aging) {
                hurt(ModDamageTypes.oldAge(level()), AGING);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), getX(), getY(0.7), getZ(),
                    16, 1.0, 1.0, 1.0, 0.05);
            } else if (getHealth() < getMaxHealth()) {
                heal(REGEN);
            }
        }
        if (!isBare() && getHealth() < getMaxHealth() / 2) {
            shedPlates(server);
        }
        if (!calledAtTwoThirds && getHealth() < getMaxHealth() * 2 / 3) {
            calledAtTwoThirds = true;
            callGuardians(server);
        }
        if (!calledAtOneThird && getHealth() < getMaxHealth() / 3) {
            calledAtOneThird = true;
            callGuardians(server);
        }
        tickStrikes(server);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        bossEvent.setColor(aging ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);

        int strike = strikeTicks();
        if (strike == STRIKE_TICKS - STRIKE_HIT) {
            ringBell(server);
        }
        if (stompTicks() == STOMP_TICKS - STOMP_HIT) {
            stomp(server);
        }
        if (breathTicks() == BREATH_TICKS / 2) {
            breathe(server);
        }
        bellCooldown--;
        breathCooldown--;
        stompCooldown--;
        stormCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || busy()) {
            return;
        }
        double distance = distanceTo(target);
        if (stompCooldown <= 0 && distance < STOMP_RADIUS) {
            startMove(DATA_STOMP, STOMP_TICKS);
            stompCooldown = 100;
        } else if (bellCooldown <= 0) {
            startMove(DATA_STRIKE, STRIKE_TICKS);
            bellCooldown = isBare() ? 140 : 200;
        } else if (breathCooldown <= 0 && distance > 3.0 && distance < BREATH_RANGE && hasLineOfSight(target)) {
            startMove(DATA_BREATH, BREATH_TICKS);
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            playSound(ModSounds.COLOSSUS_BREATH, 3.0F, 1.0F);
            breathCooldown = 260;
        } else if (isBare() && stormCooldown <= 0) {
            callStorm(server);
            stormCooldown = 220;
        }
    }

    private void countDown(EntityDataAccessor<Integer> data) {
        int value = entityData.get(data);
        if (value > 0) {
            entityData.set(data, value - 1);
        }
    }

    private void startMove(EntityDataAccessor<Integer> data, int ticks) {
        entityData.set(data, ticks);
        getNavigation().stop();
    }

    // ------------------------------------------------------------------ Fähigkeiten

    /** Glockenschlag: stößt alles in der Nähe zurück und stellt die Zeit richtig – Zeitzonen in der Nähe lösen sich auf. */
    public void ringBell(ServerLevel level) {
        playSound(ModSounds.COLOSSUS_BELL, 5.0F, isBare() ? 0.85F : 1.0F);
        for (double r = 2.0; r <= BELL_RADIUS; r += 2.0) {
            int points = (int) (r * 6);
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * r, getY() + 0.3, getZ() + Math.sin(angle) * r, 1, 0, 0.05, 0, 0.0);
            }
        }
        AABB area = getBoundingBox().inflate(BELL_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && !(e instanceof RuinGuardian) && e.isAlive())) {
            double d = victim.distanceTo(this);
            if (d > BELL_RADIUS) {
                continue;
            }
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.4 * (1.0 - d / BELL_RADIUS) + 0.3);
            victim.hurt(damageSources().mobAttack(this), 6.0F);
            victim.push(away.x, 0.4, away.z);
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0), this);
        }
        // Die Glocke stellt die Zeit richtig
        ServerTimeEngine engine = ServerTimeEngine.of(level);
        for (TimeZone zone : List.copyOf(engine.zones())) {
            AABB bounds = zone.shape().bounds();
            if (!zone.shape().isGlobal() && bounds != null && bounds.intersects(area)) {
                engine.removeZone(zone.id());
            }
        }
    }

    /** Stampfen: Wer zu nah steht, fliegt. */
    private void stomp(ServerLevel level) {
        playSound(ModSounds.COLOSSUS_STOMP, 4.0F, 1.0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPosition().below())), getX(), getY() + 0.1, getZ(),
            80, STOMP_RADIUS / 2, 0.1, STOMP_RADIUS / 2, 0.2);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(STOMP_RADIUS),
            e -> e != this && !(e instanceof RuinGuardian) && e.isAlive() && e.onGround())) {
            if (victim.distanceTo(this) <= STOMP_RADIUS + getBbWidth() / 2) {
                victim.hurt(damageSources().mobAttack(this), 10.0F);
                Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.6);
                victim.push(away.x, 0.75, away.z);
                victim.hurtMarked = true;
            }
        }
    }

    /** Verwitterungshauch: ein Kegel aus Staub und Asche, der Rüstungen altern lässt und schwächt. */
    private void breathe(ServerLevel level) {
        Vec3 look = getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 origin = position().add(0, getBbHeight() * 0.75, 0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BREATH_RANGE),
            e -> e != this && !(e instanceof RuinGuardian) && e.isAlive())) {
            Vec3 to = victim.position().add(0, victim.getBbHeight() / 2, 0).subtract(origin);
            double distance = to.length();
            if (distance > BREATH_RANGE || to.multiply(1, 0, 1).normalize().dot(look) < 0.5) {
                continue;
            }
            victim.hurt(ModDamageTypes.oldAge(level), 4.0F);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
            if (victim instanceof Player player) {
                weatherArmor(player);
            }
        }
    }

    /** Die Jahrhunderte zehren an der Rüstung: jedes Teil verliert einen Teil seiner Haltbarkeit. */
    static void weatherArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isDamageableItem()) {
                int wear = Math.max(1, Math.round(stack.getMaxDamage() * WEATHERING));
                stack.hurtAndBreak(wear, player, owner -> owner.broadcastBreakEvent(slot));
            }
        }
    }

    /** Ruft zwei Ruinenwächter aus dem Boden. */
    private void callGuardians(ServerLevel level) {
        playSound(ModSounds.COLOSSUS_BELL, 4.0F, 0.6F);
        Player nearest = level.getNearestPlayer(this, 32.0);
        for (int side : new int[] {-1, 1}) {
            Vec3 dir = getLookAngle().multiply(1, 0, 1).normalize().yRot(side * Mth.HALF_PI);
            BlockPos at = BlockPos.containing(position().add(dir.scale(5.0)));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
            RuinGuardian guardian = ModEntities.RUIN_GUARDIAN.create(level);
            if (guardian == null) {
                continue;
            }
            guardian.moveTo(at.getX() + 0.5, y, at.getZ() + 0.5, getYRot(), 0.0F);
            guardian.setPost(new BlockPos(at.getX(), y, at.getZ()));
            level.addFreshEntity(guardian);
            guardian.wake();
            if (nearest != null) {
                guardian.alarm(nearest);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_STONE_BRICKS.defaultBlockState()), guardian.getX(),
                guardian.getY() + 0.5, guardian.getZ(), 40, 0.8, 0.8, 0.8, 0.1);
        }
    }

    /** Zweite Phase: Die Kupferplatten fallen ab. */
    private void shedPlates(ServerLevel level) {
        entityData.set(DATA_BARE, true);
        playSound(ModSounds.COLOSSUS_HURT, 4.0F, 0.6F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OXIDIZED_COPPER.defaultBlockState()), getX(), getY(0.8), getZ(),
            120, 1.2, 1.0, 1.2, 0.2);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        stormCooldown = 60;
    }

    /** Gewitter: Über jedem Gegner unter freiem Himmel kündigt sich ein Blitz an. */
    private void callStorm(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24), p -> p.isAlive() && !p.isSpectator())) {
            BlockPos pos = player.blockPosition();
            if (level.canSeeSky(pos)) {
                strikes.add(new Strike(player.position(), 30));
            }
        }
    }

    private void tickStrikes(ServerLevel level) {
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike strike = it.next();
            if (strike.ticks % 5 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.at.x, strike.at.y + 0.2, strike.at.z, 12, 0.8, 0.1, 0.8, 0.05);
            }
            if (--strike.ticks <= 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(strike.at.x, strike.at.y, strike.at.z);
                    level.addFreshEntity(bolt);
                }
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ Kampf

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(getLookAngle().scale(0.6)).add(0, 0.5, 0));
            target.hurtMarked = true;
            playSound(ModSounds.COLOSSUS_STOMP, 1.5F, 1.4F);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (emergeTicks() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        if (source.is(ModDamageTypes.OLD_AGE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return super.hurt(source, amount * (isAging() ? FAST_FACTOR : ENDURING_FACTOR));
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.COLOSSUS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COLOSSUS_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
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
        tag.putBoolean("Bare", isBare());
        tag.putBoolean("CalledTwoThirds", calledAtTwoThirds);
        tag.putBoolean("CalledOneThird", calledAtOneThird);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_BARE, tag.getBoolean("Bare"));
        calledAtTwoThirds = tag.getBoolean("CalledTwoThirds");
        calledAtOneThird = tag.getBoolean("CalledOneThird");
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    /** Für Tests: sofort die Glocke schlagen lassen. */
    public void ringNow() {
        if (level() instanceof ServerLevel server) {
            ringBell(server);
        }
    }
}
