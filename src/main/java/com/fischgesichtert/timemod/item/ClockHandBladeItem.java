package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Zeigerklinge – der Minutenzeiger einer Turmuhr, geschliffen zur Waffe der Zeitwächter. Wen sie
 * trifft, dessen Zeit gerät ins Stocken: Wesen laufen zwei Sekunden lang mit halber Zeit, Spieler
 * werden langsamer.
 */
public class ClockHandBladeItem extends SwordItem {
    public static final int SLOW_TICKS = 40;
    public static final double SLOW_FACTOR = 0.5;

    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 1200;
        }

        @Override
        public float getSpeed() {
            return 8.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 3.0F;
        }

        @Override
        public int getLevel() {
            return 3;
        }

        @Override
        public int getEnchantmentValue() {
            return 18;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.BRASS_INGOT);
        }
    };

    public ClockHandBladeItem(Properties properties) {
        super(TIER, 3, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel level && target.isAlive()) {
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0), attacker);
            } else if (!TimeEngine.isOutsideTime(target)) {
                ServerTimeEngine engine = ServerTimeEngine.of(level);
                // Nur einmal zugleich: Wer schon in einer Zone steckt, behält sie
                if (engine.resolveEntity(target) == null) {
                    engine.addEntityZone(target, SLOW_FACTOR, TimeFilter.ALL_ENTITIES, attacker.getUUID(), SLOW_TICKS);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
