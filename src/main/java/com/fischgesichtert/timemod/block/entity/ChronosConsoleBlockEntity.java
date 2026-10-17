package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.ChronosPortalBlock;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononCellItem;
import com.fischgesichtert.timemod.energy.ChrononStorage;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.gate.ChronosGate;
import com.fischgesichtert.timemod.item.EraFocusItem;
import com.fischgesichtert.timemod.menu.ChronosConsoleMenu;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Die Konsole des Chronos-Tors: Hier wird das Zieljahr gewählt, Chronon gesammelt und das Tor
 * aufgeladen. Sie findet den Ring in ihrer Nähe selbst und öffnet nach zehn Sekunden das Portal.
 */
public class ChronosConsoleBlockEntity extends BlockEntity implements ChrononStorage {
    public static final int CAPACITY = 500_000;
    public static final int CHARGE_TICKS = 200;
    public static final int OPEN_TICKS = 600;
    public static final int SEARCH_RADIUS = 8;
    /** So viel Chronon fließt pro Tick aus der Zelle in den Speicher (eine volle Paradox-Zelle in drei Sekunden). */
    private static final int CELL_FLOW = 2_500;
    private static final int NEIGHBOR_FLOW = 500;
    public static final int SLOT_CELL = 0;
    public static final int SLOT_FOCUS = 1;
    private static final Component TITLE = Component.translatable("container.timemod.chronos_console");

    public enum Phase {
        IDLE, CHARGING, OPEN
    }

    /** Zustand des Rings für Anzeige und Menü. */
    public enum GateState {
        MISSING, READY, BLOCKED, NO_TIME
    }

    private final SimpleContainer items = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            ChronosConsoleBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == SLOT_CELL ? stack.getItem() instanceof ChrononCellItem : stack.getItem() instanceof EraFocusItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> Eras.indexOf(target());
                case 1 -> buffer & 0xFFFF;
                case 2 -> buffer >>> 16;
                case 3 -> phase.ordinal();
                case 4 -> progress;
                case 5 -> gateState().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ChronosConsoleMenu.DATA_COUNT;
        }
    };

    private int target = -1;
    private int buffer;
    private Phase phase = Phase.IDLE;
    /** Beim Aufladen: Ticks seit dem Start; offen: verbleibende Ticks. */
    private int progress;
    /** Index der Epoche, auf die das Tor gerade lädt oder offen ist. */
    private int activeEra = -1;
    private int paid;
    @Nullable
    private ChronosGate.Shape gate;
    private boolean gateBlocked;
    // Nur im Client: Stellung der Ringe und Leuchten der Öffnung
    private double ringAngle;
    private double lastRingAngle;
    private float glow;
    private float lastGlow;

    public ChronosConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONOS_CONSOLE, pos, state);
    }

    // ------------------------------------------------------------------ Ticken

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChronosConsoleBlockEntity self) {
        if (level.getGameTime() % 20 == pos.asLong() % 20 || (self.gate == null && level.getGameTime() % 5 == 0)) {
            self.refreshGate();
        }
        self.absorbChronon();
        switch (self.phase) {
            case CHARGING -> self.tickCharging((ServerLevel) level);
            case OPEN -> self.tickOpen((ServerLevel) level);
            default -> {
            }
        }
    }

    /** Der Client zählt selbst weiter, damit die Ringe ohne ständige Pakete flüssig beschleunigen. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ChronosConsoleBlockEntity self) {
        if (self.phase == Phase.CHARGING && self.progress < CHARGE_TICKS) {
            self.progress++;
        } else if (self.phase == Phase.OPEN && self.progress > 0) {
            self.progress--;
        }
        self.lastRingAngle = self.ringAngle;
        self.ringAngle += self.ringSpeed();
        if (self.ringAngle > 720_000) {
            self.ringAngle -= 720_000;
            self.lastRingAngle -= 720_000;
        }
        self.lastGlow = self.glow;
        float wanted = switch (self.phase) {
            case CHARGING -> self.progress / (float) CHARGE_TICKS;
            case OPEN -> 1.0F;
            default -> 0.0F;
        };
        self.glow += (wanted - self.glow) * 0.15F;
    }

    /** Drehung der Zahnrad-Ringe in Grad pro Tick: ruhig im Leerlauf, rasend beim Aufladen. */
    private float ringSpeed() {
        if (gate == null) {
            return 0.0F;
        }
        return switch (phase) {
            case CHARGING -> {
                float charge = progress / (float) CHARGE_TICKS;
                yield 0.25F + 30.0F * charge * charge;
            }
            case OPEN -> 12.0F * Math.min(1.0F, progress / 60.0F) + 0.25F;
            default -> 0.25F;
        };
    }

    public float ringAngle(float partialTick) {
        return (float) (lastRingAngle + (ringAngle - lastRingAngle) * partialTick);
    }

    public float glow(float partialTick) {
        return lastGlow + (glow - lastGlow) * partialTick;
    }

    private void refreshGate() {
        if (level == null) {
            return;
        }
        ChronosGate.Shape previous = gate;
        if (gate == null || !ChronosGate.isFrameComplete(level, gate)) {
            gate = ChronosGate.find(level, worldPosition, SEARCH_RADIUS);
        }
        boolean blocked = gate != null && phase == Phase.IDLE && !ChronosGate.isOpeningClear(level, gate);
        if (!java.util.Objects.equals(previous, gate) || blocked != gateBlocked) {
            gateBlocked = blocked;
            sync();
        }
    }

    private void absorbChronon() {
        if (buffer >= CAPACITY || level == null) {
            return;
        }
        ItemStack cell = items.getItem(SLOT_CELL);
        if (!cell.isEmpty()) {
            int moved = Chronon.extract(cell, Math.min(CELL_FLOW, CAPACITY - buffer));
            if (moved > 0) {
                buffer += moved;
                items.setChanged();
            }
        }
        // Kondensatoren und andere Speicher direkt daneben speisen die Konsole mit
        if (level.getGameTime() % 5 == 0) {
            for (Direction direction : Direction.values()) {
                if (buffer >= CAPACITY) {
                    break;
                }
                // Nicht von anderen Konsolen: Die würden sich nur gegenseitig leer- und volltanken
                if (level.getBlockEntity(worldPosition.relative(direction)) instanceof ChrononStorage storage
                    && !(storage instanceof ChronosConsoleBlockEntity)) {
                    buffer += storage.drainChronon(Math.min(NEIGHBOR_FLOW * 5, CAPACITY - buffer));
                    setChanged();
                }
            }
        }
    }

    private void tickCharging(ServerLevel level) {
        progress++;
        if (gate == null || (progress % 10 == 0 && (!ChronosGate.isFrameComplete(level, gate) || !ChronosGate.isOpeningClear(level, gate)))) {
            abort(level);
            return;
        }
        // Funken laufen spiralförmig zur Mitte, immer schneller
        float charge = progress / (float) CHARGE_TICKS;
        Vec3 center = gate.center();
        int sparks = 1 + (int) (charge * 6);
        for (int i = 0; i < sparks; i++) {
            double angle = (progress * (0.2 + charge) + i * Mth.TWO_PI / sparks) % Mth.TWO_PI;
            double radius = 3.6 * (1.0 - 0.6 * level.random.nextDouble() * charge);
            Vec3 at = onRing(gate, center, angle, radius);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (progress % 20 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 4 + (int) (charge * 20), 0.4, 0.4, 0.4, 0.05 + 0.1 * charge);
        }
        if (progress >= CHARGE_TICKS) {
            open(level);
        }
    }

    private static Vec3 onRing(ChronosGate.Shape gate, Vec3 center, double angle, double radius) {
        double sideways = Math.cos(angle) * radius;
        double up = Math.sin(angle) * radius;
        return gate.axis() == Direction.Axis.X ? center.add(sideways, up, 0) : center.add(0, up, sideways);
    }

    private void tickOpen(ServerLevel level) {
        progress--;
        if (progress <= 0 || gate == null || (progress % 10 == 0 && !ChronosGate.isFrameComplete(level, gate))) {
            close(level);
        }
    }

    // ------------------------------------------------------------------ Bedienung

    /** Die Epoche, in der die Konsole steht – oder null, wenn hier keine Zeitreise möglich ist (Nether, End). */
    @Nullable
    public Era currentEra() {
        return level == null ? null : Eras.byLevel(level.dimension());
    }

    public Era target() {
        Era here = currentEra();
        if (target >= 0 && target < Eras.all().size() && (here == null || Eras.all().get(target) != here)) {
            return Eras.all().get(target);
        }
        return Eras.step(here == null ? Eras.PRESENT : here, -1);
    }

    /** Pfeiltasten: 0 = ◀◀, 1 = ◀, 2 = ▶, 3 = ▶▶. Die eigene Epoche wird übersprungen. */
    public void select(int button) {
        if (phase != Phase.IDLE) {
            return;
        }
        int direction = button < 2 ? -1 : 1;
        boolean jump = button == 0 || button == 3;
        Era here = currentEra();
        Era next = jump ? Eras.jump(target(), direction) : Eras.step(target(), direction);
        if (next == here) {
            Era beyond = Eras.step(next, direction);
            next = beyond == here ? target() : beyond;
        }
        target = Eras.indexOf(next);
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.CONSOLE_CLICK, SoundSource.BLOCKS, 0.6F, jump ? 0.8F : 1.1F);
        }
    }

    /** Chronon für eine Reise: steigt logarithmisch mit dem Abstand der Jahre. */
    public static int cost(Era from, Era to) {
        long distance = Math.abs((long) to.years() - from.years());
        if (distance == 0) {
            return 0;
        }
        double chronon = 10_000 + 15_000 * Math.log10(1 + distance);
        return (int) (Math.round(chronon / 500.0) * 500);
    }

    public ItemStack focus() {
        return items.getItem(SLOT_FOCUS);
    }

    /** Zellen- und Fokus-Platz (auch für Trichter und andere Mods). */
    public SimpleContainer items() {
        return items;
    }

    public GateState gateState() {
        if (currentEra() == null) {
            return GateState.NO_TIME;
        }
        if (gate == null) {
            return GateState.MISSING;
        }
        return gateBlocked ? GateState.BLOCKED : GateState.READY;
    }

    /** Lädt das Tor auf; liefert den Grund, falls es nicht geht. */
    @Nullable
    public Component charge(Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (phase != Phase.IDLE) {
            return Component.translatable("message.timemod.gate.busy");
        }
        Era here = currentEra();
        if (here == null) {
            return Component.translatable("message.timemod.gate.no_time");
        }
        refreshGate();
        if (gate == null) {
            return Component.translatable("message.timemod.gate.missing");
        }
        if (!ChronosGate.isOpeningClear(serverLevel, gate)) {
            return Component.translatable("message.timemod.gate.blocked");
        }
        Era to = target();
        if (!EraFocusItem.allows(focus(), to.age())) {
            return Component.translatable("message.timemod.gate.focus");
        }
        int cost = player.getAbilities().instabuild ? 0 : cost(here, to);
        if (buffer < cost) {
            return Component.translatable("message.timemod.gate.chronon", Chronon.format(cost - buffer));
        }
        buffer -= cost;
        paid = cost;
        phase = Phase.CHARGING;
        progress = 0;
        activeEra = Eras.indexOf(to);
        Vec3 center = gate.center();
        serverLevel.playSound(null, center.x, center.y, center.z, ModSounds.GATE_CHARGE, SoundSource.BLOCKS, 2.0F, 1.0F);
        setChanged();
        sync();
        return null;
    }

    private void abort(ServerLevel level) {
        buffer = Math.min(CAPACITY, buffer + paid);
        paid = 0;
        phase = Phase.IDLE;
        progress = 0;
        activeEra = -1;
        level.playSound(null, worldPosition, ModSounds.GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.4F);
        setChanged();
        sync();
    }

    private void open(ServerLevel level) {
        BlockState portal = ModBlocks.CHRONOS_PORTAL.defaultBlockState().setValue(ChronosPortalBlock.AXIS, gate.axis())
            .setValue(ChronosPortalBlock.ERA, activeEra);
        for (BlockPos pos : gate.opening()) {
            level.setBlock(pos, portal, Block.UPDATE_CLIENTS);
        }
        phase = Phase.OPEN;
        progress = OPEN_TICKS;
        paid = 0;
        Vec3 center = gate.center();
        level.playSound(null, center.x, center.y, center.z, ModSounds.GATE_OPEN, SoundSource.BLOCKS, 3.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 80, 1.2, 1.2, 1.2, 0.25);
        Advancement advancement = level.getServer().getAdvancements().getAdvancement(TimeMod.id("time_gate"));
        if (advancement != null) {
            for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(center) < 32 * 32)) {
                player.getAdvancements().award(advancement, "done");
            }
        }
        setChanged();
        sync();
    }

    /** Schließt das Portal und entfernt die Öffnung. */
    public void close(ServerLevel level) {
        if (gate != null) {
            for (BlockPos pos : gate.opening()) {
                if (level.getBlockState(pos).is(ModBlocks.CHRONOS_PORTAL)) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            Vec3 center = gate.center();
            level.playSound(null, center.x, center.y, center.z, ModSounds.GATE_CLOSE, SoundSource.BLOCKS, 2.0F, 1.0F);
        }
        phase = Phase.IDLE;
        progress = 0;
        activeEra = -1;
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ Menü und Abbau

    public void openMenu(Player player) {
        if (level == null) {
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
            new ChronosConsoleMenu(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), this), TITLE));
    }

    /** Beim Abbau: Inhalt fallen lassen und ein offenes Portal schließen. */
    public void onBroken() {
        if (level instanceof ServerLevel serverLevel) {
            Containers.dropContents(level, worldPosition, items);
            if (phase == Phase.OPEN) {
                close(serverLevel);
            } else if (phase == Phase.CHARGING) {
                phase = Phase.IDLE;
            }
        }
    }

    public Phase phase() {
        return phase;
    }

    public int progress() {
        return progress;
    }

    public int buffer() {
        return buffer;
    }

    @Nullable
    public ChronosGate.Shape gate() {
        return gate;
    }

    /** Die Epoche, auf die das Tor lädt oder offen ist; sonst das gewählte Ziel. */
    public Era activeEra() {
        return activeEra >= 0 && activeEra < Eras.all().size() ? Eras.all().get(activeEra) : target();
    }

    /** Speicher und Zelle zählen für die Chronon-Bilanz beim Zurückspulen – sonst gäbe es Reisen zum Nulltarif. */
    @Override
    public int storedChronon() {
        return (int) Math.min(Integer.MAX_VALUE, (long) buffer + Chronon.stored(items.getItem(SLOT_CELL)));
    }

    @Override
    public int drainChronon(int amount) {
        int fromBuffer = Math.min(buffer, Math.max(0, amount));
        buffer -= fromBuffer;
        int fromCell = Chronon.extract(items.getItem(SLOT_CELL), amount - fromBuffer);
        setChanged();
        return fromBuffer + fromCell;
    }

    /** Für Tests: Chronon direkt in den Speicher legen. */
    public void addChronon(int amount) {
        buffer = Math.max(0, Math.min(CAPACITY, buffer + amount));
        setChanged();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("Target", target);
        tag.putInt("Chronon", buffer);
        tag.putString("Phase", phase.name());
        tag.putInt("Progress", progress);
        tag.putInt("ActiveEra", activeEra);
        tag.putInt("Paid", paid);
        tag.putBoolean("Blocked", gateBlocked);
        if (gate != null) {
            tag.put("Gate", gate.save());
        }
        tag.put("Cell", items.getItem(SLOT_CELL).save(new CompoundTag()));
        tag.put("Focus", items.getItem(SLOT_FOCUS).save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        target = tag.getInt("Target");
        buffer = tag.getInt("Chronon");
        try {
            phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException e) {
            phase = Phase.IDLE;
        }
        progress = tag.getInt("Progress");
        activeEra = tag.contains("ActiveEra") ? tag.getInt("ActiveEra") : -1;
        paid = tag.getInt("Paid");
        gateBlocked = tag.getBoolean("Blocked");
        gate = tag.contains("Gate") ? ChronosGate.Shape.load(tag.getCompound("Gate")) : null;
        items.setItem(SLOT_CELL, ItemStack.of(tag.getCompound("Cell")));
        items.setItem(SLOT_FOCUS, ItemStack.of(tag.getCompound("Focus")));
    }

    /** Für den Client nur, was Ringe und Anzeige brauchen – die Gegenstände kennt er über das Menü. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Target", target);
        tag.putString("Phase", phase.name());
        tag.putInt("Progress", progress);
        tag.putInt("ActiveEra", activeEra);
        tag.putBoolean("Blocked", gateBlocked);
        if (gate != null) {
            tag.put("Gate", gate.save());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
