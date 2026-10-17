package com.fischgesichtert.timemod.chronos;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.item.ChrononToolItem;
import com.fischgesichtert.timemod.item.ModeChrononToolItem;
import com.fischgesichtert.timemod.item.RewindCapable;
import com.fischgesichtert.timemod.item.RewindPersistent;
import com.fischgesichtert.timemod.item.TimeTargeting;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Die Chronos-Uhr: ein Werkzeug für alles. Anhalten (bis 10 s, Radius bis 64 oder die ganze Welt),
 * vorspulen (bis 10×, global bis 5×), verlangsamen (bis 0,05×) und zurückspulen. Beim Zurückspulen
 * läuft die Welt sichtbar rückwärts, solange man die Taste hält; Loslassen hält sie genau dort an.
 * Jede Benutzung bringt Meisterschaft, die die Grenzen erweitert. Schleichen und Benutzen hebt die
 * eigenen Zeitzonen wieder auf.
 */
public class ChronosWatchItem extends ChrononToolItem implements RewindCapable, RewindPersistent {
    public static final int CAPACITY = 200_000;
    /** So weit „sieht“ die Uhr für Zonen in Blickrichtung. */
    private static final double REACH = 48.0;
    /** Ersatz-Radius für globale Wirkung bei der Kostenberechnung. */
    private static final int GLOBAL_COST_RADIUS = 96;

    public ChronosWatchItem(Properties properties) {
        super(CAPACITY, properties.fireResistant());
    }

    // ------------------------------------------------------------------ Kosten

    /** Größe der Wirkung relativ zu Radius 12 (gedämpft: doppelter Radius kostet knapp das Dreifache). */
    private static double size(ChronosSettings s) {
        double r = s.global ? GLOBAL_COST_RADIUS : s.radius;
        return Math.pow(r / 12.0, 1.5);
    }

    /** Was kostet die aktuelle Einstellung? (Zurückspulen: für die volle Tiefe.) */
    public static int cost(ChronosSettings s) {
        double size = size(s);
        double base = switch (s.mode) {
            case STOP -> 180.0 * size * s.stopSeconds;
            case FAST -> 150.0 * Math.pow(s.factor() - 1.0, 1.5) * size * s.seconds() / 30.0;
            case SLOW -> 100.0 * (1.0 / s.factor() - 1.0) * size * s.seconds() / 30.0;
            case REWIND -> 600.0 * size * Math.pow(s.rewindSeconds / 10.0, 2);
        };
        return (int) Math.ceil(base * s.efficiency());
    }

    /** Zurückspulen um {@code seconds}: wird mit dem Quadrat der Tiefe teurer. */
    public static int rewindCost(ChronosSettings s, double seconds) {
        return (int) Math.ceil(600.0 * size(s) * Math.pow(seconds / 10.0, 2) * s.efficiency());
    }

    // ------------------------------------------------------------------ Benutzen

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ChronosSettings s = ChronosSettings.of(stack);
        if (player.isSecondaryUseActive()) {
            if (level instanceof ServerLevel serverLevel) {
                cancelZones(serverLevel, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (s.mode == ChronosSettings.Mode.REWIND) {
            return startScrubbing(level, player, stack, hand, s);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(player, stack, cost(s))) {
            return InteractionResultHolder.fail(stack);
        }
        ServerTimeEngine engine = ServerTimeEngine.of(serverLevel);
        int mask = s.effectiveMask();
        int ticks = s.seconds() * 20;
        if (s.mode == ChronosSettings.Mode.STOP) {
            ZoneShape shape = s.global ? ZoneShape.Global.INSTANCE : new ZoneShape.Sphere(player.getEyePosition(), s.radius);
            engine.addZone(shape, 0.0, mask, player.getUUID(), ticks);
            player.getCooldowns().addCooldown(this, ticks + 40);
            gainMastery(stack, 3);
        } else {
            ZoneShape shape = s.global ? ZoneShape.Global.INSTANCE : new ZoneShape.Sphere(TimeTargeting.point(player, REACH), s.radius);
            engine.addZone(shape, s.factor(), mask, player.getUUID(), ticks);
            player.getCooldowns().addCooldown(this, 20);
            gainMastery(stack, 2);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Alle eigenen Zeitzonen aufheben: Die Zeit läuft wieder normal. */
    private static void cancelZones(ServerLevel level, Player player) {
        ServerTimeEngine engine = ServerTimeEngine.of(level);
        boolean any = false;
        for (TimeZone zone : new ArrayList<>(engine.zones())) {
            if (!zone.isRewind() && player.getUUID().equals(zone.owner())) {
                engine.removeZone(zone.id());
                any = true;
            }
        }
        if (any) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TIME_RESUME, SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.timemod.chronos.cancelled").withStyle(ChatFormatting.AQUA), true);
        }
    }

    // ------------------------------------------------------------------ Zurückspulen mit Scrubbing

    private InteractionResultHolder<ItemStack> startScrubbing(Level level, Player player, ItemStack stack, InteractionHand hand,
                                                              ChronosSettings s) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            ChronoJournal journal = ServerTimeEngine.of(serverLevel).journal();
            if (!journal.isRecording() || journal.availableTicks() < 10 || journal.session() != null) {
                player.displayClientMessage(Component.translatable("message.timemod.rewind.nothing").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            int ticks = Math.min(s.rewindSeconds * 20, journal.availableTicks());
            int paid = rewindCost(s, ticks / 20.0);
            if (!pay(player, stack, paid)) {
                return InteractionResultHolder.fail(stack);
            }
            int speed = Mth.clamp((int) Math.ceil(ticks / 120.0), 2, 8);
            RewindSession.Region region = s.global ? null : new RewindSession.Region(player.position(), s.radius);
            boolean free = !s.self;
            RewindSession session = journal.rewind(ticks, speed, region, player.getUUID(), s.effectiveMask(), free);
            if (session == null) {
                if (!player.getAbilities().instabuild) {
                    Chronon.insert(stack, Chronon.discounted(player, paid));
                }
                return InteractionResultHolder.fail(stack);
            }
            if (free) {
                session.whileHeld(p -> p.isUsingItem() && p.getUseItem().getItem() instanceof ChronosWatchItem);
            }
            session.onFinish(() -> finishScrubbing(serverPlayer, s, paid, session.done()));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Wer früher loslässt, bekommt den nicht genutzten Teil zurück. Gutgeschrieben wird der Uhr, die
     * der Spieler jetzt hält, denn das Zurückspulen hat sein Inventar mit zurückgedreht.
     */
    private static void finishScrubbing(ServerPlayer player, ChronosSettings settings, int paid, int doneTicks) {
        ItemStack watch = held(player);
        if (watch.isEmpty()) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            int used = rewindCost(settings, doneTicks / 20.0);
            int unused = Math.max(0, Chronon.discounted(player, paid) - Chronon.discounted(player, used));
            if (unused > 0) {
                Chronon.insert(watch, unused);
            }
        }
        gainMastery(watch, 1 + doneTicks / 40);
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof ChronosWatchItem) {
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level instanceof ServerLevel serverLevel) {
            RewindSession session = ServerTimeEngine.of(serverLevel).journal().session();
            if (session != null && entity.getUUID().equals(session.owner())) {
                session.stop();
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.TIME_RESUME, SoundSource.PLAYERS, 1.0F, 1.3F);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Die Chronos-Uhr in der Hand (Haupthand zuerst), sonst die erste im Inventar. */
    public static ItemStack held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ChronosWatchItem) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ChronosWatchItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Die Uhr nur, wenn sie wirklich in einer Hand liegt (für HUD und Mausrad). */
    public static ItemStack inHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ChronosWatchItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ Meisterschaft

    public static void gainMastery(ItemStack stack, int points) {
        ChronosSettings s = ChronosSettings.of(stack);
        int before = s.level();
        s.mastery += points;
        s.save(stack);
        if (s.level() > before) {
            stack.getOrCreateTag().putBoolean("LevelUp", true);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && stack.hasTag() && stack.getTag().getBoolean("LevelUp")) {
            stack.getTag().remove("LevelUp");
            ChronosSettings s = ChronosSettings.of(stack);
            player.displayClientMessage(Component.translatable("message.timemod.chronos.mastery", Component.translatable("chronos.timemod.level." + s.level()))
                .withStyle(ChatFormatting.GOLD), false);
            level.playSound(null, player.blockPosition(), ModSounds.BOSS_AWAKEN, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
    }

    /** Meisterschaft und Einstellungen sind Erfahrung, kein Weltzustand: Sie überstehen jedes Zurückspulen. */
    @Override
    public void carryThroughRewind(ItemStack current, ItemStack restored) {
        CompoundTag settings = current.getTagElement(ChronosSettings.KEY);
        if (settings != null && ChronosSettings.of(current).mastery >= ChronosSettings.of(restored).mastery) {
            restored.getOrCreateTag().put(ChronosSettings.KEY, settings.copy());
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ChronosSettings s = ChronosSettings.of(stack);
        tooltip.add(Component.translatable("tooltip.timemod.chronon", Chronon.format(Chronon.stored(stack)), Chronon.format(CAPACITY))
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(describe(s).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.timemod.chronos.cost", Chronon.format(cost(s))).withStyle(ChatFormatting.GRAY));
        if (s.mask != 0) {
            tooltip.add(Component.translatable("tooltip.timemod.affects", Modules.describe(s.mask)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.timemod.chronos.mastery", Component.translatable("chronos.timemod.level." + s.level()),
            Math.round(s.levelProgress() * 100)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.timemod.chronos.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** „Stopp · Radius 24 · 5 s“ und Ähnliches – für Tooltip und HUD. */
    public static Component describe(ChronosSettings s) {
        Component where = s.global ? Component.translatable("tooltip.timemod.chronos.global")
            : Component.translatable("tooltip.timemod.chronos.radius", s.radius);
        Component mode = modeName(s.mode);
        return switch (s.mode) {
            case STOP -> Component.translatable("tooltip.timemod.chronos.stop", mode, where, s.stopSeconds);
            case FAST, SLOW -> Component.translatable("tooltip.timemod.chronos.zone", mode, ModeChrononToolItem.format(s.factor()), where, s.seconds());
            case REWIND -> Component.translatable("tooltip.timemod.chronos.rewind", mode, where, s.rewindSeconds);
        };
    }

    public static Component modeName(ChronosSettings.Mode mode) {
        return Component.translatable("chronos.timemod.mode." + mode.name().toLowerCase(Locale.ROOT));
    }
}
