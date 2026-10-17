package com.fischgesichtert.timemod.entity;

import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Chrono-Wurfmesser im Flug. In gestoppter Zeit bleibt es einfach in der Luft hängen – und fliegt
 * weiter, sobald die Zeit wieder läuft (dafür sorgt die Zeit-Engine von selbst).
 */
public class ThrownChronoKnife extends AbstractArrow {
    private ItemStack knife = new ItemStack(ModItems.CHRONO_KNIFE);

    public ThrownChronoKnife(EntityType<? extends ThrownChronoKnife> type, Level level) {
        super(type, level);
        setBaseDamage(3.0);
    }

    public ThrownChronoKnife(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.CHRONO_KNIFE, owner, level);
        this.knife = stack.copyWithCount(1);
        setBaseDamage(3.0);
    }

    @Override
    protected ItemStack getPickupItem() {
        return knife.copy();
    }

    public ItemStack knife() {
        return knife;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Knife", knife.save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Knife")) {
            knife = ItemStack.of(tag.getCompound("Knife"));
        }
    }
}
