package org.bukkit.inventory;

import org.bukkit.entity.HumanEntity;

import java.util.List;

/**
 * Minimal compile-time stub of org.bukkit.inventory.Inventory.
 */
public interface Inventory {

    int getSize();

    ItemStack getItem(int index);

    void setItem(int index, ItemStack item);

    List<ItemStack> getContents();

    List<HumanEntity> getViewers();

    InventoryHolder getHolder();
}
