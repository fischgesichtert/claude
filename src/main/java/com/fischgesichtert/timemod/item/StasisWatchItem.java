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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stillstand-Uhr: hält die Zeit im Umkreis von 12 Blöcken für 2 Sekunden an. Nur du bewegst dich. */
public class StasisWatchItem extends ChrononToolItem implements ModularTool {
    public static final double RADIUS = 12;
    public static final int DURATION = 2 * 20;
    public static final int COST = 400;
    public static final int COOLDOWN = 10 * 20;
    /** Ohne Filtermodul hält die Uhr alles an – auch fremde Spieler. */
    public static final int DEFAULT_MASK = TimeFilter.DEFAULT | TimeFilter.PLAYERS;

    public StasisWatchItem(Properties properties) {
        super(4000, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Modules.Effects fx = Modules.effects(stack);
        if (!pay(player, stack, fx.cost(COST))) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine.of(serverLevel).addZone(new ZoneShape.Sphere(player.getEyePosition(), fx.radius(RADIUS)), 0.0,
            fx.mask(DEFAULT_MASK), player.getUUID(), fx.duration(DURATION));
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int moduleSlots() {
        return 3;
    }

    @Override
    public boolean accepts(ModuleItem.Kind kind) {
        return kind == ModuleItem.Kind.FILTER || kind == ModuleItem.Kind.RADIUS || kind == ModuleItem.Kind.DURATION || kind == ModuleItem.Kind.EFFICIENCY
            || kind == ModuleItem.Kind.DAMPING;
    }

    @Override
    public List<Component> moduleStats(ItemStack stack) {
        Modules.Effects fx = Modules.effects(stack);
        return Modules.stats(fx, Component.translatable("tooltip.timemod.stats", Modules.number(fx.radius(RADIUS)),
            Modules.number(fx.duration(DURATION) / 20.0), fx.cost(COST)));
    }
}
