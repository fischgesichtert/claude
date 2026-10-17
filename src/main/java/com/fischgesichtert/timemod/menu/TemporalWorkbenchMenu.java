package com.fischgesichtert.timemod.menu;

import com.fischgesichtert.timemod.block.entity.TemporalWorkbenchBlockEntity;
import com.fischgesichtert.timemod.module.ModularTool;
import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModMenus;
import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Temporal-Werkbank: links das Werkzeug, daneben seine Modulplätze. Die Module existieren nur als
 * Eintrag im Werkzeug – die Plätze sind eine Ansicht darauf, damit nichts verdoppelt werden kann.
 */
public class TemporalWorkbenchMenu extends AbstractContainerMenu {
    public static final int MODULE_SLOTS = 3;
    public static final int TOOL_X = 17;
    public static final int TOOL_Y = 35;
    public static final int MODULE_X = 53;
    public static final int[] MODULE_Y = {17, 35, 53};
    private static final int INV_START = 1 + MODULE_SLOTS;
    private static final int INV_END = INV_START + 36;

    private final Container toolContainer;
    private final ModuleView modules;
    private final ContainerLevelAccess access;
    private final Player player;
    @Nullable
    private final TemporalWorkbenchBlockEntity workbench;

    public TemporalWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), ContainerLevelAccess.NULL, null);
    }

    public TemporalWorkbenchMenu(int id, Inventory inventory, Container toolContainer, ContainerLevelAccess access,
                                 @Nullable TemporalWorkbenchBlockEntity workbench) {
        super(ModMenus.TEMPORAL_WORKBENCH, id);
        this.toolContainer = toolContainer;
        this.access = access;
        this.workbench = workbench;
        this.player = inventory.player;
        this.modules = new ModuleView();
        addSlot(new ToolSlot(toolContainer, TOOL_X, TOOL_Y));
        for (int i = 0; i < MODULE_SLOTS; i++) {
            addSlot(new ModuleSlot(modules, i, MODULE_X, MODULE_Y[i]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public boolean isFor(TemporalWorkbenchBlockEntity blockEntity) {
        return workbench == blockEntity;
    }

    public ItemStack tool() {
        return toolContainer.getItem(0);
    }

    /** Wie viele Modulplätze das eingelegte Werkzeug hat (0 ohne Werkzeug). */
    public int activeModuleSlots() {
        return tool().getItem() instanceof ModularTool modular ? Math.min(MODULE_SLOTS, modular.moduleSlots()) : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TEMPORAL_WORKBENCH)
            && access.evaluate((level, pos) -> level.getBlockState(pos).getValue(com.fischgesichtert.timemod.block.TemporalWorkbenchBlock.FORMED), true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (workbench != null) {
            workbench.release(player);
        }
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
        } else if (stack.getItem() instanceof ModularTool) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ModuleItem) {
            if (!moveOneModule(stack)) {
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

    /**
     * Ein Modul in den ersten passenden freien Platz legen. Nicht über {@code moveItemStackTo}:
     * das würde gleiche Module zu einem Stapel zusammenschieben, obwohl ein Platz nur eins hält.
     */
    private boolean moveOneModule(ItemStack stack) {
        for (int i = 1; i < INV_START; i++) {
            Slot slot = slots.get(i);
            if (!slot.hasItem() && slot.mayPlace(stack)) {
                slot.setByPlayer(stack.split(1));
                return true;
            }
        }
        return false;
    }

    /** Werkzeugplatz: nur Werkzeuge mit Modulplätzen, eins zur Zeit. */
    private final class ToolSlot extends Slot {
        ToolSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ModularTool;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Modulplatz: aktiv, solange das Werkzeug so viele Plätze hat und das Modul versteht. */
    private final class ModuleSlot extends Slot {
        private final int index;

        ModuleSlot(ModuleView view, int index, int x, int y) {
            super(view, index, x, y);
            this.index = index;
        }

        @Override
        public boolean isActive() {
            return index < activeModuleSlots();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive() || !(tool().getItem() instanceof ModularTool modular) || !(stack.getItem() instanceof ModuleItem module)
                || !modular.accepts(module.kind())) {
                return false;
            }
            if (module.kind() == ModuleItem.Kind.FILTER) {
                for (int i = 0; i < MODULE_SLOTS; i++) {
                    if (i != index && modules.getItem(i).is(module)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!player.level().isClientSide) {
                player.level().playSound(null, player.blockPosition(), ModSounds.CLOCK_WIND, SoundSource.BLOCKS, 0.5F, 1.6F);
            }
        }
    }

    /**
     * Ansicht auf die Modulliste des eingelegten Werkzeugs. Wechselt das Werkzeug (anderes
     * Objekt im Werkzeugplatz), wird neu eingelesen; jede Änderung wird sofort zurückgeschrieben.
     */
    private final class ModuleView implements Container {
        private final NonNullList<ItemStack> items = NonNullList.withSize(MODULE_SLOTS, ItemStack.EMPTY);
        private ItemStack loadedFrom = ItemStack.EMPTY;

        private void refresh() {
            ItemStack current = tool();
            if (current == loadedFrom) {
                return;
            }
            loadedFrom = current;
            List<ItemStack> stored = current.getItem() instanceof ModularTool ? Modules.slots(current, MODULE_SLOTS) : List.of();
            for (int i = 0; i < MODULE_SLOTS; i++) {
                items.set(i, i < stored.size() ? stored.get(i) : ItemStack.EMPTY);
            }
        }

        private void writeBack() {
            ItemStack current = tool();
            if (current.getItem() instanceof ModularTool modular) {
                List<ItemStack> slotsToSave = new ArrayList<>();
                for (int i = 0; i < modular.moduleSlots() && i < MODULE_SLOTS; i++) {
                    slotsToSave.add(items.get(i));
                }
                Modules.write(current, slotsToSave);
                toolContainer.setChanged();
            }
        }

        @Override
        public int getContainerSize() {
            return MODULE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            refresh();
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            refresh();
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            refresh();
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) {
                writeBack();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            refresh();
            ItemStack removed = ContainerHelper.takeItem(items, slot);
            if (!removed.isEmpty()) {
                writeBack();
            }
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            refresh();
            items.set(slot, stack);
            writeBack();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            // Leere Stapel (nach Verschieben) aufräumen und speichern
            boolean dirty = false;
            for (int i = 0; i < MODULE_SLOTS; i++) {
                if (!items.get(i).isEmpty() && items.get(i).getCount() <= 0) {
                    items.set(i, ItemStack.EMPTY);
                    dirty = true;
                }
            }
            if (dirty) {
                writeBack();
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < MODULE_SLOTS; i++) {
                items.set(i, ItemStack.EMPTY);
            }
        }
    }
}
