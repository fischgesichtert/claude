package com.fischgesichtert.timemod.command;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Admin-Befehle für die Zeit-Engine.
 * <pre>
 * /chrono zone &lt;radius&gt; &lt;faktor&gt; [sekunden]
 * /chrono box &lt;von&gt; &lt;bis&gt; &lt;faktor&gt; [sekunden]
 * /chrono global &lt;faktor&gt; [sekunden]
 * /chrono daytime|weather &lt;faktor&gt; [sekunden]
 * /chrono clear | list
 * /chrono travel &lt;jahre&gt;   (durch den Zeitstrom, ohne Tor; 0 = Gegenwart)
 * </pre>
 */
public final class ChronoCommand {
    private ChronoCommand() {
    }

    /** Reist durch den Zeitstrom in eine der Stufen der Zeitmaschine – für Admins und Kartenbauer. */
    private static int travel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int years = IntegerArgumentType.getInteger(ctx, "years");
        Era era = Eras.byYears(years);
        if (era == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.timemod.travel.unknown", years));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!TimeStream.depart(player, era, null, player.blockPosition(), !era.isPresent())) {
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.timemod.travel.done", era.yearText()), false);
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chrono")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("zone")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5, 128))
                    .then(withDuration(Commands.argument("factor", DoubleArgumentType.doubleArg(0, TimeZone.MAX_FACTOR)), (ctx, ticks) -> {
                        double radius = DoubleArgumentType.getDouble(ctx, "radius");
                        return create(ctx, new ZoneShape.Sphere(ctx.getSource().getPosition(), radius), TimeFilter.DEFAULT, ticks);
                    }))))
            .then(Commands.literal("box")
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .then(withDuration(Commands.argument("factor", DoubleArgumentType.doubleArg(0, TimeZone.MAX_FACTOR)), (ctx, ticks) -> {
                            BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
                            BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
                            AABB box = new AABB(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()),
                                Math.max(from.getX(), to.getX()) + 1, Math.max(from.getY(), to.getY()) + 1, Math.max(from.getZ(), to.getZ()) + 1);
                            return create(ctx, new ZoneShape.Box(box), TimeFilter.DEFAULT, ticks);
                        })))))
            .then(Commands.literal("global")
                .then(withDuration(Commands.argument("factor", DoubleArgumentType.doubleArg(0, 5)), (ctx, ticks) ->
                    create(ctx, ZoneShape.Global.INSTANCE, TimeFilter.DEFAULT, ticks))))
            .then(Commands.literal("daytime")
                .then(withDuration(Commands.argument("factor", DoubleArgumentType.doubleArg(0, 100)), (ctx, ticks) ->
                    create(ctx, ZoneShape.Global.INSTANCE, TimeFilter.DAYTIME, ticks))))
            .then(Commands.literal("weather")
                .then(withDuration(Commands.argument("factor", DoubleArgumentType.doubleArg(0, 100)), (ctx, ticks) ->
                    create(ctx, ZoneShape.Global.INSTANCE, TimeFilter.WEATHER, ticks))))
            .then(Commands.literal("record")
                .then(Commands.literal("on")
                    .executes(ctx -> record(ctx, true, ChronoJournal.DEFAULT_SECONDS))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
                        .executes(ctx -> record(ctx, true, IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.literal("off").executes(ctx -> record(ctx, false, 0))))
            .then(Commands.literal("rewind")
                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.05, 300))
                    .executes(ctx -> rewind(ctx, 2))
                    .then(Commands.argument("speed", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> rewind(ctx, IntegerArgumentType.getInteger(ctx, "speed"))))))
            .then(Commands.literal("clear").executes(ctx -> {
                ServerTimeEngine.of(ctx.getSource().getLevel()).clear();
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.timemod.chrono.clear"), true);
                return 1;
            }))
            .then(Commands.literal("list").executes(ChronoCommand::list))
            .then(Commands.literal("travel")
                .then(Commands.argument("years", IntegerArgumentType.integer()).executes(ChronoCommand::travel))));
    }

    @FunctionalInterface
    private interface ZoneAction {
        int run(CommandContext<CommandSourceStack> ctx, int durationTicks) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> withDuration(ArgumentBuilder<CommandSourceStack, ?> factorArg, ZoneAction action) {
        return factorArg
            .executes(ctx -> action.run(ctx, -1))
            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                .executes(ctx -> action.run(ctx, IntegerArgumentType.getInteger(ctx, "seconds") * 20)));
    }

    private static int create(CommandContext<CommandSourceStack> ctx, ZoneShape shape, int mask, int ticks) {
        CommandSourceStack source = ctx.getSource();
        double factor = DoubleArgumentType.getDouble(ctx, "factor");
        Entity entity = source.getEntity();
        UUID owner = entity != null ? entity.getUUID() : null;
        int id = ServerTimeEngine.of(source.getLevel()).addZone(shape, factor, mask, owner, ticks).id();
        source.sendSuccess(() -> Component.translatable("commands.timemod.chrono.created", id, format(factor)), true);
        return id;
    }

    private static int record(CommandContext<CommandSourceStack> ctx, boolean on, int seconds) {
        ChronoJournal journal = ServerTimeEngine.of(ctx.getSource().getLevel()).journal();
        if (on) {
            journal.setSeconds(seconds);
        }
        journal.setRecording(on);
        ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.timemod.chrono.record.on" : "commands.timemod.chrono.record.off", seconds), true);
        return 1;
    }

    /** speed = zurückgespulte Ticks pro Tick (0 = sofort). */
    private static int rewind(CommandContext<CommandSourceStack> ctx, int speed) {
        ChronoJournal journal = ServerTimeEngine.of(ctx.getSource().getLevel()).journal();
        int ticks = (int) Math.round(DoubleArgumentType.getDouble(ctx, "seconds") * 20);
        if (!journal.isRecording() || journal.availableTicks() == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.timemod.chrono.rewind.nothing"));
            return 0;
        }
        Entity entity = ctx.getSource().getEntity();
        journal.rewind(ticks, speed, null, entity != null ? entity.getUUID() : null);
        int actual = Math.min(ticks, journal.availableTicks());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.timemod.chrono.rewind.started", String.format(Locale.ROOT, "%.1f", ticks / 20.0)), true);
        return actual;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerTimeEngine engine = ServerTimeEngine.of(ctx.getSource().getLevel());
        if (engine.zones().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.timemod.chrono.none"), false);
            return 0;
        }
        for (TimeZone zone : engine.zones()) {
            String seconds = zone.remainingTicks() < 0 ? "∞" : String.valueOf(zone.remainingTicks() / 20);
            ctx.getSource().sendSuccess(() -> Component.literal("#" + zone.id() + "  " + describe(zone.shape()) + "  " + format(zone.factor()) + "×  " + seconds + " s"), false);
        }
        return engine.zones().size();
    }

    private static String describe(ZoneShape shape) {
        if (shape instanceof ZoneShape.Sphere sphere) {
            return String.format(Locale.ROOT, "Kugel r=%.1f @ %.1f %.1f %.1f", sphere.radius(), sphere.center().x, sphere.center().y, sphere.center().z);
        }
        if (shape instanceof ZoneShape.Box box) {
            AABB b = box.box();
            return String.format(Locale.ROOT, "Quader %.0f %.0f %.0f → %.0f %.0f %.0f", b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
        }
        return "global";
    }

    private static String format(double factor) {
        return factor == Math.rint(factor) ? String.valueOf((long) factor) : String.format(Locale.ROOT, "%.2f", factor);
    }
}
