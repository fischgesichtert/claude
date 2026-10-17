package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.safety.Safety;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Die Zeitschleife: ein Talisman für das Ende des Spiels. Stirbst du, während du sie trägst, spult
 * die Welt um dich herum 10 Sekunden zurück – mit dir. Das kostet 100k Chronon, danach braucht die
 * Schleife 30 Minuten, bis sie wieder bereit ist. Ein Totem in der Hand hat Vorrang.
 */
public class TimeLoopItem extends ChrononToolItem implements RewindCapable, RewindPersistent {
    public static final int CAPACITY = 150_000;
    public static final int COST = 100_000;
    public static final int COOLDOWN_TICKS = 30 * 60 * 20;
    public static final int REWIND_TICKS = 10 * 20;
    public static final double RADIUS = 24.0;
    private static final String READY_AT = "LoopReadyAt";
    /** Spieler, deren Rettung angefordert ist oder gerade läuft. */
    private static final Set<UUID> RESCUING = new HashSet<>();

    public TimeLoopItem(Properties properties) {
        super(CAPACITY, properties.fireResistant());
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
            Safety.test("Zeitschleife", () -> !(entity instanceof ServerPlayer player && tryLoop(player, source)), true));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RESCUING.clear());
    }

    /** Fängt den Tod ab, wenn eine bereite, geladene Schleife getragen wird. */
    static boolean tryLoop(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // Die Rettung läuft schon: bis die Zeit zurückgedreht ist, stirbt hier niemand
        if (RESCUING.contains(player.getUUID())) {
            player.setHealth(1.0F);
            return true;
        }
        if (holdsTotem(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ItemStack loop = readyLoop(player, level.getGameTime());
        if (loop.isEmpty()) {
            return false;
        }
        ChronoJournal journal = ServerTimeEngine.of(level).journal();
        if (!journal.isRecording() || journal.availableTicks() < 20 || journal.session() != null) {
            return false;
        }
        if (!Chronon.pay(player, loop, COST)) {
            return false;
        }
        loop.getOrCreateTag().putLong(READY_AT, level.getGameTime() + COOLDOWN_TICKS);
        // Das Zurückspulen setzt das alte Leben wieder ein – bis dahin wenigstens eins
        player.setHealth(1.0F);
        RewindSession session = journal.rewind(REWIND_TICKS, 4, new RewindSession.Region(player.position(), RADIUS), player.getUUID(),
            RewindSession.EVERYTHING, false);
        if (session != null) {
            RESCUING.add(player.getUUID());
            session.onFinish(() -> RESCUING.remove(player.getUUID()));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TIME_STOP, SoundSource.PLAYERS, 2.0F, 0.75F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 120, 0.6, 1.0, 0.6, 0.2);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.timemod.loop.title").withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.timemod.loop.subtitle").withStyle(ChatFormatting.GRAY)));
        Advancement advancement = level.getServer().getAdvancements().getAdvancement(TimeMod.id("loop"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "done");
        }
        return true;
    }

    private static boolean holdsTotem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }
        return false;
    }

    /** Die erste bereite Schleife im Inventar (zweite Hand eingeschlossen). */
    private static ItemStack readyLoop(Player player, long now) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof TimeLoopItem && isReady(stack, now)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isReady(ItemStack stack, long now) {
        return !stack.hasTag() || stack.getTag().getLong(READY_AT) <= now;
    }

    /** Die Abklingzeit ist keine Weltgeschichte: Sie übersteht das eigene Zurückspulen. */
    @Override
    public void carryThroughRewind(ItemStack current, ItemStack restored) {
        if (current.hasTag() && current.getTag().contains(READY_AT)) {
            restored.getOrCreateTag().putLong(READY_AT, Math.max(current.getTag().getLong(READY_AT),
                restored.hasTag() ? restored.getTag().getLong(READY_AT) : 0));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.timemod.loop.cost", Chronon.format(COST)).withStyle(ChatFormatting.GRAY));
        if (level != null && !isReady(stack, level.getGameTime())) {
            long minutes = (stack.getTag().getLong(READY_AT) - level.getGameTime() + 1199) / 1200;
            tooltip.add(Component.translatable("tooltip.timemod.loop.cooldown", minutes).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.timemod.loop.ready").withStyle(ChatFormatting.GREEN));
        }
    }
}
