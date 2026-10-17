package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import com.fischgesichtert.timemod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Moosbär – ein Bär der fernen Zukunft, auf dessen Rücken ein ganzer kleiner Wald wächst: Moos, Farn,
 * Pilze. Er lebt in alten Wäldern und überwucherten Ruinen ab +100 Jahren. Friedlich, solange man ihn
 * und seine Jungen in Ruhe lässt; mit Beeren lässt er sich locken und züchten. Mit der Schere lässt
 * sich sein Moos ernten – es wächst in ein paar Minuten nach.
 */
public class MossBear extends PolarBear implements Shearable {
    private static final EntityDataAccessor<Boolean> DATA_SHEARED = SynchedEntityData.defineId(MossBear.class, EntityDataSerializers.BOOLEAN);
    /** Bis das Moos nachgewachsen ist (Ticks). */
    public static final int REGROW_TICKS = 6000;
    /** Ab welchem Jahr der Zukunft Moosbären leben. */
    public static final int FROM_YEARS = 100;
    private static final Ingredient BERRIES = Ingredient.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES);

    private int regrow;

    public MossBear(EntityType<? extends MossBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PolarBear.createAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    /** Nur in der Zukunft ab +100 Jahren, auf Gras, Moos oder Waldboden, im Hellen. */
    public static boolean checkMossBearSpawnRules(EntityType<MossBear> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                                  RandomSource random) {
        Era era = Eras.byLevel(level.getLevel().dimension());
        if (reason != MobSpawnType.SPAWN_EGG && reason != MobSpawnType.COMMAND && (era == null || !era.isFuture() || era.years() < FROM_YEARS)) {
            return false;
        }
        var ground = level.getBlockState(pos.below());
        return (ground.is(BlockTags.ANIMALS_SPAWNABLE_ON) || ground.is(BlockTags.DIRT) || ground.is(Blocks.MOSS_BLOCK))
            && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_SHEARED, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, BERRIES, false));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return BERRIES.test(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MOSS_BEAR.create(level);
    }

    public boolean isSheared() {
        return entityData.get(DATA_SHEARED);
    }

    public void setSheared(boolean sheared) {
        entityData.set(DATA_SHEARED, sheared);
        regrow = sheared ? REGROW_TICKS : 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (!level().isClientSide && readyForShearing()) {
                shear(SoundSource.PLAYERS);
                gameEvent(GameEvent.SHEAR, player);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, "moss_bear");
                }
                stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(hand));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void shear(SoundSource source) {
        level().playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 0.8F);
        setSheared(true);
        int moss = 1 + random.nextInt(2);
        for (int i = 0; i < moss; i++) {
            drop(new ItemStack(Items.MOSS_BLOCK));
        }
        if (random.nextFloat() < 0.3F) {
            drop(new ItemStack(random.nextBoolean() ? Items.FERN : Items.BROWN_MUSHROOM));
        }
        if (random.nextFloat() < 0.1F) {
            drop(new ItemStack(Items.FLOWERING_AZALEA));
        }
    }

    private void drop(ItemStack stack) {
        ItemEntity item = spawnAtLocation(stack, 1.0F);
        if (item != null) {
            item.setDeltaMovement(item.getDeltaMovement().add((random.nextFloat() - random.nextFloat()) * 0.1F, random.nextFloat() * 0.05F,
                (random.nextFloat() - random.nextFloat()) * 0.1F));
        }
    }

    @Override
    public boolean readyForShearing() {
        return isAlive() && !isSheared() && !isBaby();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isSheared() && --regrow <= 0) {
            setSheared(false);
        }
    }

    @Override
    public float getVoicePitch() {
        return (random.nextFloat() - random.nextFloat()) * 0.2F + (isBaby() ? 1.3F : 0.8F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Sheared", isSheared());
        tag.putInt("Regrow", regrow);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_SHEARED, tag.getBoolean("Sheared"));
        regrow = tag.getInt("Regrow");
    }
}
