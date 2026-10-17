package com.fischgesichtert.timemod.entity.boss;

import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.StopResistant;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Paradox – dein eigenes Echo im Zeitstrom. Es trägt Kopien deiner Rüstung und deiner besten Waffe
 * und benutzt deine Zeitkräfte gegen dich: Es hält die Zeit an und legt einen Kranz aus Messern um
 * dich, der losfliegt, sobald die Zeit weiterläuft. Es springt hinter dich, und es spiegelt deine
 * Zeitzonen: Wer es verlangsamt oder anhält, trifft sich selbst. Es lässt sich nicht anhalten. Ab der
 * Hälfte ruft es Echos herbei. Gegenmittel nach JoJo-Regel: In voller Chrono-Rüstung bewegst du dich
 * auch in seiner angehaltenen Zeit.
 */
public class ParadoxBoss extends Monster implements StopResistant {
    private static final EntityDataAccessor<Optional<UUID>> DATA_ECHO_OF = SynchedEntityData.defineId(ParadoxBoss.class,
        EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_ECHO_NAME = SynchedEntityData.defineId(ParadoxBoss.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_STOPPING = SynchedEntityData.defineId(ParadoxBoss.class, EntityDataSerializers.BOOLEAN);

    public static final int STOP_TICKS = 60;
    public static final double STOP_RADIUS = 18.0;
    public static final int KNIFE_RING = 6;
    public static final float PROJECTILE_FACTOR = 0.5F;
    public static final int MIRROR_COOLDOWN = 100;
    public static final int MAX_ECHOES = 4;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<Integer> seenZones = new HashSet<>();
    private int stopCooldown = 160;
    private int knifeCooldown = 70;
    private int blinkCooldown = 120;
    private int echoCooldown = 60;
    private int mirrorCooldown;
    private int stopLeft;
    private boolean secondPhase;
    /** Nur Client: frühere Positionen für die Nachbilder. */
    public final Vec3[] trail = new Vec3[12];
    public int trailHead;

    public ParadoxBoss(EntityType<? extends ParadoxBoss> type, Level level) {
        super(type, level);
        xpReward = 120;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 400.0)
            .add(Attributes.MOVEMENT_SPEED, 0.33)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ECHO_OF, Optional.empty());
        entityData.define(DATA_ECHO_NAME, "");
        entityData.define(DATA_STOPPING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ParadoxEcho.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ------------------------------------------------------------------ Echo

    /** Wird zum Echo dieses Spielers: Kopien seiner Rüstung und seiner besten Waffe (nicht fallen zu lassen). */
    public void becomeEchoOf(Player player) {
        entityData.set(DATA_ECHO_OF, Optional.of(player.getUUID()));
        entityData.set(DATA_ECHO_NAME, player.getGameProfile().getName());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                setItemSlot(slot, player.getItemBySlot(slot).copy());
                setDropChance(slot, 0.0F);
            }
        }
        setItemSlot(EquipmentSlot.MAINHAND, bestWeapon(player).copy());
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        bossEvent.setName(echoName());
    }

    /** Die Waffe mit dem höchsten Angriffsschaden aus dem Inventar des Spielers. */
    static ItemStack bestWeapon(Player player) {
        ItemStack best = ItemStack.EMPTY;
        double bestDamage = 0;
        for (ItemStack stack : player.getInventory().items) {
            double damage = 0;
            for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                damage += modifier.getAmount();
            }
            if (damage > bestDamage) {
                bestDamage = damage;
                best = stack;
            }
        }
        return best;
    }

    @Nullable
    public UUID echoOf() {
        return entityData.get(DATA_ECHO_OF).orElse(null);
    }

    public String echoPlayerName() {
        return entityData.get(DATA_ECHO_NAME);
    }

    public Component echoName() {
        String name = echoPlayerName();
        return name.isEmpty() ? getDisplayName() : Component.translatable("entity.timemod.paradox.echo_of", name);
    }

    /** Hält Paradox gerade selbst die Zeit an? (Stärkeres Leuchten im Client.) */
    public boolean isStoppingTime() {
        return entityData.get(DATA_STOPPING);
    }

    public boolean isSecondPhase() {
        return secondPhase;
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) {
                trail[trailHead] = position();
                trailHead = (trailHead + 1) % trail.length;
            }
            if (random.nextInt(isStoppingTime() ? 1 : 4) == 0) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (stopLeft > 0) {
            stopLeft--;
            if (stopLeft == STOP_TICKS - 10 && getTarget() != null && !isNoAi()) {
                knifeRing(getTarget(), KNIFE_RING);
            }
            if (stopLeft == 0) {
                entityData.set(DATA_STOPPING, false);
            }
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        // Spiegeln ist seine Natur, keine Entscheidung – das passiert auch ohne KI
        if (mirrorCooldown > 0) {
            mirrorCooldown--;
        }
        if (tickCount % 5 == 0) {
            mirrorZones();
        }
        // Ohne KI keine Angriffe (Vorführungen, Tests)
        if (isNoAi()) {
            return;
        }
        focusOnOriginal();
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            if (--stopCooldown <= 0 && distanceTo(target) < STOP_RADIUS - 3) {
                timeStop();
                stopCooldown = secondPhase ? 200 : 300;
            }
            if (stopLeft == 0 && --knifeCooldown <= 0 && hasLineOfSight(target) && distanceToSqr(target) > 16) {
                throwKnives(target, secondPhase ? 7 : 5);
                knifeCooldown = secondPhase ? 70 : 110;
            }
            if (--blinkCooldown <= 0 && distanceToSqr(target) > 36) {
                blinkBehind(target);
                blinkCooldown = secondPhase ? 110 : 170;
            }
            if (secondPhase && --echoCooldown <= 0) {
                summonEchoes(3);
                echoCooldown = 500;
            }
        }
        if (!secondPhase && getHealth() < getMaxHealth() / 2) {
            enterSecondPhase();
        }
    }

    /** Das Echo jagt zuerst den, dessen Echo es ist. */
    private void focusOnOriginal() {
        UUID original = echoOf();
        if (original == null || tickCount % 20 != 0) {
            return;
        }
        Player player = level().getPlayerByUUID(original);
        if (player != null && player.isAlive() && !player.getAbilities().instabuild && !player.isSpectator() && distanceToSqr(player) < 48 * 48
            && getTarget() != player) {
            setTarget(player);
        }
    }

    private void enterSecondPhase() {
        secondPhase = true;
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38);
        playSound(ModSounds.PARADOX_VOICE, 3.0F, 0.8F);
        say(Component.translatable("boss.timemod.paradox.later"));
        summonEchoes(2);
    }

    private void say(Component text) {
        for (ServerPlayer player : ((ServerLevel) level()).getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
            player.displayClientMessage(Component.literal("Paradox: ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(text.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)), false);
        }
    }

    /** „Die Zeit gehört mir“: Stillstand um Paradox, kurz darauf ein Messerkranz um das Ziel. */
    public void timeStop() {
        ServerTimeEngine.of((ServerLevel) level()).addZone(new ZoneShape.Sphere(position().add(0, 1, 0), STOP_RADIUS), 0.0,
            TimeFilter.DEFAULT | TimeFilter.PLAYERS, getUUID(), STOP_TICKS);
        stopLeft = STOP_TICKS;
        entityData.set(DATA_STOPPING, true);
        playSound(ModSounds.PARADOX_VOICE, 2.0F, 1.2F);
    }

    public int stopTicksLeft() {
        return stopLeft;
    }

    /** Messer im Kreis um das Ziel – in angehaltener Zeit bleiben sie in der Luft hängen. */
    public void knifeRing(LivingEntity target, int count) {
        Vec3 center = target.getEyePosition();
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + random.nextDouble() * 0.3;
            Vec3 at = center.add(Math.cos(angle) * 3.5, (random.nextDouble() - 0.4) * 1.2, Math.sin(angle) * 3.5);
            Vec3 dir = center.subtract(at).normalize();
            ThrownChronoKnife knife = knife();
            knife.setPos(at.x, at.y, at.z);
            knife.shoot(dir.x, dir.y, dir.z, 1.4F, 0.0F);
            level().addFreshEntity(knife);
            ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
        }
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.TRIDENT_THROW, 1.5F, 1.6F);
    }

    private ThrownChronoKnife knife() {
        ThrownChronoKnife knife = new ThrownChronoKnife(level(), this, new ItemStack(ModItems.CHRONO_KNIFE));
        knife.pickup = AbstractArrow.Pickup.DISALLOWED;
        knife.setBaseDamage(2.0);
        return knife;
    }

    private void throwKnives(LivingEntity target, int count) {
        Vec3 from = getEyePosition().subtract(0, 0.2, 0);
        Vec3 aim = target.getEyePosition().subtract(from).normalize();
        for (int i = 0; i < count; i++) {
            double angle = (i - (count - 1) / 2.0) * 0.12;
            Vec3 dir = aim.yRot((float) angle);
            ThrownChronoKnife knife = knife();
            knife.setPos(from.x + dir.x * 0.6, from.y, from.z + dir.z * 0.6);
            knife.shoot(dir.x, dir.y + 0.04, dir.z, 1.7F, 1.0F);
            level().addFreshEntity(knife);
        }
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.TRIDENT_THROW, 1.5F, 1.3F);
    }

    /** Zeitsprung hinter das Ziel. */
    public void blinkBehind(LivingEntity target) {
        Vec3 back = target.getLookAngle().multiply(1, 0, 1);
        if (back.lengthSqr() < 1.0E-4) {
            back = new Vec3(1, 0, 0);
        }
        Vec3 to = target.position().subtract(back.normalize().scale(2.5));
        ServerLevel level = (ServerLevel) level();
        Vec3 from = position();
        if (randomTeleport(to.x, to.y + 1, to.z, false)) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 40, 0.3, 0.8, 0.3, 0.1);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.3, 0.8, 0.3, 0.1);
            getLookControl().setLookAt(target);
            playSound(ModSounds.PARADOX_BLINK, 1.6F, 1.0F);
        }
    }

    private void summonEchoes(int count) {
        ServerLevel level = (ServerLevel) level();
        int alive = level.getEntitiesOfClass(ParadoxEcho.class, getBoundingBox().inflate(48)).size();
        for (int i = 0; i < Math.min(count, MAX_ECHOES - alive); i++) {
            ParadoxEcho echo = ModEntities.PARADOX_ECHO.create(level);
            if (echo == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / count + random.nextDouble();
            echo.moveTo(getX() + Math.cos(angle) * 2, getY(), getZ() + Math.sin(angle) * 2, getYRot(), 0);
            echo.copyFrom(this);
            echo.setTarget(getTarget());
            level.addFreshEntity(echo);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, echo.getX(), echo.getY() + 1, echo.getZ(), 30, 0.3, 0.8, 0.3, 0.1);
        }
        playSound(ModSounds.PARADOX_BLINK, 2.0F, 0.6F);
    }

    // ------------------------------------------------------------------ Spiegel

    /**
     * Spiegelt neue Zeitzonen von Spielern in der Nähe: Zeitstopp und Zeitlupe treffen den Spieler
     * selbst, Zeitraffer macht Paradox schneller. Zurückspulen heilt das Echo ein Stück.
     */
    private void mirrorZones() {
        ServerTimeEngine engine = ServerTimeEngine.of((ServerLevel) level());
        Set<Integer> current = new HashSet<>();
        for (TimeZone zone : engine.zones()) {
            current.add(zone.id());
            if (!seenZones.add(zone.id()) || zone.owner() == null || zone.owner().equals(getUUID())) {
                continue;
            }
            Player owner = level().getPlayerByUUID(zone.owner());
            if (owner == null || owner.getAbilities().instabuild || owner.isSpectator() || owner.distanceToSqr(this) > 32 * 32) {
                continue;
            }
            if (zone.isRewind()) {
                heal(getMaxHealth() * 0.15F);
                playSound(ModSounds.REWIND, 1.5F, 1.4F);
                continue;
            }
            if (mirrorCooldown > 0 || zone.factor() == 1.0) {
                continue;
            }
            mirror(engine, zone, owner);
        }
        seenZones.retainAll(current);
    }

    private void mirror(ServerTimeEngine engine, TimeZone zone, Player owner) {
        if (zone.factor() > 1.0) {
            engine.addEntityZone(this, zone.factor(), TimeFilter.ALL_ENTITIES, getUUID(), 80);
        } else {
            engine.addEntityZone(owner, zone.factor(), TimeFilter.PLAYERS, getUUID(), zone.isStop() ? 50 : 80);
        }
        mirrorCooldown = MIRROR_COOLDOWN;
        owner.displayClientMessage(Component.translatable("message.timemod.paradox.mirror").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        playSound(ModSounds.PARADOX_VOICE, 1.5F, 1.5F);
    }

    // ------------------------------------------------------------------ Kampf

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount *= PROJECTILE_FACTOR;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            for (ParadoxEcho echo : level.getEntitiesOfClass(ParadoxEcho.class, new AABB(blockPosition()).inflate(64))) {
                echo.shatter();
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 200, 0.6, 1.0, 0.6, 0.3);
            say(Component.translatable("boss.timemod.paradox.defeated"));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CLOCK_TICK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PARADOX_VOICE;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F;
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
        UUID original = echoOf();
        if (original != null) {
            tag.putUUID("EchoOf", original);
        }
        tag.putString("EchoName", echoPlayerName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        secondPhase = tag.getBoolean("SecondPhase");
        if (tag.hasUUID("EchoOf")) {
            entityData.set(DATA_ECHO_OF, Optional.of(tag.getUUID("EchoOf")));
        }
        entityData.set(DATA_ECHO_NAME, tag.getString("EchoName"));
        bossEvent.setName(echoName());
    }
}
