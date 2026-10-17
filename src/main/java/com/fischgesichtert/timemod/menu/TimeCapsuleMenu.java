package com.fischgesichtert.timemod.menu;

import com.fischgesichtert.timemod.block.entity.TimeCapsuleBlockEntity;
import com.fischgesichtert.timemod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Die Zeitkapsel als Fenster: neun Plätze wie eine einreihige Truhe, darunter eine Textzeile (wer sie
 * versiegelt hat oder wie man sie versiegelt), darunter das Inventar.
 */
public class TimeCapsuleMenu extends AbstractContainerMenu {
    /** So viel tiefer als in einer einreihigen Truhe liegt das Inventar: Platz für die Textzeile. */
    public static final int TEXT_HEIGHT = 12;
    private static final int SIZE = TimeCapsuleBlockEntity.SIZE;

    private final Container capsule;
    private final String sender;
    private final long years;

    /** Client: Der Inhalt kommt über die Slots, Absender und Alter über die Öffnungsdaten. */
    public TimeCapsuleMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(SIZE), buf.readUtf(), buf.readVarLong());
    }

    public TimeCapsuleMenu(int id, Inventory inventory, Container capsule, String sender, long years) {
        super(ModMenus.TIME_CAPSULE, id);
        checkContainerSize(capsule, SIZE);
        this.capsule = capsule;
        this.sender = sender;
        this.years = years;
        capsule.startOpen(inventory.player);
        for (int i = 0; i < SIZE; i++) {
            addSlot(new Slot(capsule, i, 8 + i * 18, 18));
        }
        int top = 49 + TEXT_HEIGHT;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
        }
    }

    /** Wer die Kapsel damals versiegelt hat; leer bei einer Kapsel, die noch nicht gereist ist. */
    public String sender() {
        return sender;
    }

    /** Wie viele Jahre die Kapsel unterwegs war; 0 bei einer Kapsel, die noch nicht gereist ist. */
    public long years() {
        return years;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return capsule.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        capsule.stopOpen(player);
    }
}
