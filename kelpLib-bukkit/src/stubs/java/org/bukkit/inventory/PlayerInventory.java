package org.bukkit.inventory;

import java.util.HashMap;

/**
 * Minimal compile-time stub of org.bukkit.inventory.PlayerInventory.
 */
public interface PlayerInventory extends Inventory {

    HashMap<Integer, ItemStack> addItem(ItemStack... items);

    ItemStack getItemInMainHand();

    void setItemInMainHand(ItemStack item);
}
