package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.module.ModularTool;
import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zeitlupen-Pendel: Zone mit 0,5× oder 0,25× Zeit (Radius 6, 30 s). Stärkemodule: bis 0,05×. */
public class SlowPendulumItem extends ModeChrononToolItem implements ModularTool {
    public static final double RADIUS = 6;
    public static final int DURATION = 30 * 20;
    private static final double[] BASE = {0.5, 0.25};
    private static final double[] STRONG = {0.1, 0.05};

    public SlowPendulumItem(Properties properties) {
        super(4000, properties);
    }

    /** Grundkosten: je langsamer, desto teurer (0,5× = 100, 0,25× = 300, 0,05× = 1900). */
    public static int cost(double factor) {
        return (int) Math.round(100 * (1.0 / factor - 1.0));
    }

    @Override
    protected double[] factors(ItemStack stack) {
        int strength = Math.min(STRONG.length, Modules.effects(stack).strength());
        double[] factors = new double[BASE.length + strength];
        System.arraycopy(BASE, 0, factors, 0, BASE.length);
        System.arraycopy(STRONG, 0, factors, BASE.length, strength);
        return factors;
    }

    @Override
    protected InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, double factor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Modules.Effects fx = Modules.effects(stack);
        if (!pay(player, stack, fx.cost(cost(factor)))) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine.of(serverLevel).addZone(new ZoneShape.Sphere(TimeTargeting.point(player, 24), fx.radius(RADIUS)), factor,
            fx.mask(TimeFilter.DEFAULT), player.getUUID(), fx.duration(DURATION));
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int moduleSlots() {
        return 3;
    }

    @Override
    public boolean accepts(ModuleItem.Kind kind) {
        return kind != ModuleItem.Kind.BLANK;
    }

    @Override
    public List<Component> moduleStats(ItemStack stack) {
        Modules.Effects fx = Modules.effects(stack);
        return Modules.stats(fx, Component.translatable("tooltip.timemod.stats", Modules.number(fx.radius(RADIUS)),
            Modules.number(fx.duration(DURATION) / 20.0), fx.cost(cost(factor(stack)))));
    }
}
