package com.fischgesichtert.timemod.menu;

import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity.GateState;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity.Phase;
import com.fischgesichtert.timemod.energy.ChrononCellItem;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.item.EraFocusItem;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Menü der Chronos-Konsole: Zellen-Platz, Fokus-Platz und die Pfeiltasten (als Menü-Knöpfe, damit
 * der Server jede Eingabe selbst prüft).
 */
public class ChronosConsoleMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 6;
    public static final int BUTTON_JUMP_BACK = 0;
    public static final int BUTTON_BACK = 1;
    public static final int BUTTON_FORWARD = 2;
    public static final int BUTTON_JUMP_FORWARD = 3;
    public static final int BUTTON_CHARGE = 4;
    public static final int CELL_X = 8;
    public static final int FOCUS_X = 152;
    public static final int SLOT_Y = 100;
    public static final int INVENTORY_Y = 124;
    private static final int INV_START = 2;
    private static final int INV_END = INV_START + 36;

    private final Container items;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    @Nullable
    private final ChronosConsoleBlockEntity console;

    public ChronosConsoleMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL, null);
    }

    public ChronosConsoleMenu(int id, Inventory inventory, Container items, ContainerData data, ContainerLevelAccess access,
                              @Nullable ChronosConsoleBlockEntity console) {
        super(ModMenus.CHRONOS_CONSOLE, id);
        this.items = items;
        this.data = data;
        this.access = access;
        this.console = console;
        addSlot(new Slot(items, ChronosConsoleBlockEntity.SLOT_CELL, CELL_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChrononCellItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(items, ChronosConsoleBlockEntity.SLOT_FOCUS, FOCUS_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof EraFocusItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    // ------------------------------------------------------------------ Anzeige (Client)

    public Era target() {
        int index = data.get(0);
        return index >= 0 && index < Eras.all().size() ? Eras.all().get(index) : Eras.PRESENT;
    }

    public int buffer() {
        return (data.get(1) & 0xFFFF) | (data.get(2) & 0xFFFF) << 16;
    }

    public Phase phase() {
        Phase[] phases = Phase.values();
        return phases[Math.max(0, Math.min(phases.length - 1, data.get(3)))];
    }

    public int progress() {
        return data.get(4);
    }

    public GateState gateState() {
        GateState[] states = GateState.values();
        return states[Math.max(0, Math.min(states.length - 1, data.get(5)))];
    }

    public ItemStack focus() {
        return items.getItem(ChronosConsoleBlockEntity.SLOT_FOCUS);
    }

    public boolean focusAllows(Era era) {
        return EraFocusItem.allows(focus(), era.age());
    }

    // ------------------------------------------------------------------ Eingaben (Server)

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (console == null) {
            return false;
        }
        if (id >= BUTTON_JUMP_BACK && id <= BUTTON_JUMP_FORWARD) {
            console.select(id);
            return true;
        }
        if (id == BUTTON_CHARGE) {
            Component failure = console.charge(player);
            if (failure != null) {
                player.displayClientMessage(failure.copy().withStyle(ChatFormatting.GOLD), true);
            } else if (player instanceof ServerPlayer serverPlayer) {
                // Das Menü schließen: Man soll den Ringen beim Aufladen zusehen
                serverPlayer.closeContainer();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CHRONOS_CONSOLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ChrononCellItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof EraFocusItem) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INV_START + 27) {
            if (!moveItemStackTo(stack, INV_START + 27, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_START, INV_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
