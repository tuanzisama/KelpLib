package org.bukkit.event.inventory;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

/**
 * Minimal compile-time stub of org.bukkit.event.inventory.InventoryCloseEvent.
 */
public class InventoryCloseEvent extends InventoryEvent {

    private final HumanEntity player;

    public InventoryCloseEvent(Inventory inventory, HumanEntity player) {
        super(inventory);
        this.player = player;
    }

    public HumanEntity getPlayer() {
        return player;
    }
}
