package org.bukkit.event.inventory;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Minimal compile-time stub of org.bukkit.event.inventory.InventoryClickEvent.
 */
public class InventoryClickEvent extends InventoryEvent implements Cancellable {

    private boolean cancelled;
    private final HumanEntity whoClicked;
    private final Inventory clickedInventory;
    private final ItemStack currentItem;
    private final int rawSlot;

    public InventoryClickEvent(Inventory inventory, HumanEntity whoClicked,
                               Inventory clickedInventory, ItemStack currentItem, int rawSlot) {
        super(inventory);
        this.whoClicked = whoClicked;
        this.clickedInventory = clickedInventory;
        this.currentItem = currentItem;
        this.rawSlot = rawSlot;
    }

    public HumanEntity getWhoClicked() {
        return whoClicked;
    }

    public Inventory getClickedInventory() {
        return clickedInventory;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public int getRawSlot() {
        return rawSlot;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
