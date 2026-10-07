package org.bukkit.event.inventory;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;

/**
 * Minimal compile-time stub of org.bukkit.event.inventory.InventoryEvent.
 */
public abstract class InventoryEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Inventory inventory;

    protected InventoryEvent(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
