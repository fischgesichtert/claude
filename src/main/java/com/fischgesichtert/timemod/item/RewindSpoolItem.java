package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.module.ModularTool;
import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rückspul-Spule: spult den Umkreis von 16 Blöcken bis zu 10 Sekunden zurück, sichtbar mit
 * doppelter Geschwindigkeit. Solange jemand eine Spule bei sich trägt, zeichnet die Welt auf.
 * Dauermodule spulen weiter zurück (bis 60 s), Stärkemodule schneller, Filtermodule nur Teile.
 */
public class RewindSpoolItem extends ChrononToolItem implements RewindCapable, ModularTool {
    public static final double RADIUS = 16;
    public static final int TICKS = 10 * 20;
    public static final int MAX_TICKS = 60 * 20;
    public static final int COST = 800;

    public RewindSpoolItem(Properties properties) {
        super(4000, properties);
    }

    public static int ticks(Modules.Effects fx) {
        return Math.min(MAX_TICKS, fx.duration(TICKS));
    }

    public static int speed(Modules.Effects fx) {
        return 2 + fx.strength();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        ChronoJournal journal = ServerTimeEngine.of(serverLevel).journal();
        if (!journal.isRecording() || journal.availableTicks() < 10 || journal.session() != null) {
            player.displayClientMessage(Component.translatable("message.timemod.rewind.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        Modules.Effects fx = Modules.effects(stack);
        if (!pay(player, stack, fx.cost(COST))) {
            return InteractionResultHolder.fail(stack);
        }
        journal.rewind(ticks(fx), speed(fx), new RewindSession.Region(player.position(), fx.radius(RADIUS)), player.getUUID(),
            fx.mask(RewindSession.EVERYTHING));
        player.getCooldowns().addCooldown(this, 100);
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
        return Modules.stats(fx, Component.translatable("tooltip.timemod.rewind_stats", Modules.number(fx.radius(RADIUS)),
            Modules.number(ticks(fx) / 20.0), speed(fx), fx.cost(COST)));
    }
}
