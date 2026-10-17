package com.fischgesichtert.timemod.menu;

import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.recipe.ClockmakerRecipe;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModMenus;
import com.fischgesichtert.timemod.registry.ModRecipes;
import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Uhrmacher-Werkbank: 3×3-Raster wie die Werkbank, aber für Präzisionsrezepte. */
public class ClockmakerMenu extends AbstractContainerMenu {
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public ClockmakerMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ClockmakerMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.CLOCKMAKER, id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new ResultSlotImpl(this, inventory.player, craftSlots, resultSlots, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
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

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack result = ItemStack.EMPTY;
        Optional<ClockmakerRecipe> recipe = level.getServer().getRecipeManager().getRecipeFor(ModRecipes.CLOCKMAKER, craftSlots, level);
        if (recipe.isPresent()) {
            ItemStack assembled = recipe.get().assemble(craftSlots, level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CLOCKMAKER_BENCH);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == 0) {
                access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
                if (!moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, moved);
            } else if (index >= 10 && index < 46) {
                if (!moveItemStackTo(stack, 1, 10, false)) {
                    if (index < 37) {
                        if (!moveItemStackTo(stack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(stack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!moveItemStackTo(stack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (index == 0) {
                player.drop(stack, false);
            }
        }
        return moved;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    /** Ergebnisfeld: verbraucht Zutaten nach den Regeln der Uhrmacher-Rezepte. */
    private static final class ResultSlotImpl extends Slot {
        private final ClockmakerMenu menu;
        private final CraftingContainer craftSlots;
        private final Player player;

        ResultSlotImpl(ClockmakerMenu menu, Player player, CraftingContainer craftSlots, Container result, int index, int x, int y) {
            super(result, index, x, y);
            this.menu = menu;
            this.player = player;
            this.craftSlots = craftSlots;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            stack.onCraftedBy(player.level(), player, stack.getCount());
            // Eingebaute Module der verarbeiteten Werkzeuge kommen zurück ins Inventar
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                for (ModuleItem module : Modules.installed(craftSlots.getItem(i))) {
                    this.player.getInventory().placeItemBackInInventory(new ItemStack(module));
                }
            }
            NonNullList<ItemStack> remaining = player.level().getRecipeManager().getRemainingItemsFor(ModRecipes.CLOCKMAKER, craftSlots, player.level());
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack current = craftSlots.getItem(i);
                ItemStack rest = remaining.get(i);
                if (!current.isEmpty()) {
                    craftSlots.removeItem(i, 1);
                    current = craftSlots.getItem(i);
                }
                if (!rest.isEmpty()) {
                    if (current.isEmpty()) {
                        craftSlots.setItem(i, rest);
                    } else if (ItemStack.isSameItemSameTags(current, rest)) {
                        rest.grow(current.getCount());
                        craftSlots.setItem(i, rest);
                    } else if (!this.player.getInventory().add(rest)) {
                        this.player.drop(rest, false);
                    }
                }
            }
            if (!player.level().isClientSide) {
                player.level().playSound(null, player.blockPosition(), ModSounds.CLOCK_WIND, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            menu.slotsChanged(craftSlots);
        }
    }
}
