package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.menu.TimeCapsuleMenu;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Neun Plätze für die Nachwelt. Eine gefundene Kapsel kennt ihr Alter und ihren Absender. */
public class TimeCapsuleBlockEntity extends BaseContainerBlockEntity implements ExtendedScreenHandlerFactory {
    public static final int SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private long years;
    private String sender = "";

    public TimeCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_CAPSULE, pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    /** Für gefundene Kapseln: Inhalt, Alter und wer sie damals versiegelt hat. */
    public void fill(NonNullList<ItemStack> contents, long years, String sender) {
        this.items = contents;
        this.years = years;
        this.sender = sender;
        setChanged();
    }

    public long years() {
        return years;
    }

    public String sender() {
        return sender;
    }

    /** Wie lange die Kapsel unterwegs war, etwa „1.000 Jahre“. */
    private static Component span(long years) {
        Era era = years <= Integer.MAX_VALUE ? Eras.byYears((int) -years) : null;
        return era != null ? era.span() : Component.translatable("container.timemod.time_capsule.years", years);
    }

    @Override
    protected Component getDefaultName() {
        return years > 0 ? Component.translatable("container.timemod.time_capsule.aged", span(years))
            : Component.translatable("container.timemod.time_capsule");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TimeCapsuleMenu(id, inventory, this, sender, years);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeUtf(sender);
        buf.writeVarLong(years);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putLong("Years", years);
        tag.putString("Sender", sender);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        years = tag.getLong("Years");
        sender = tag.getString("Sender");
    }
}
