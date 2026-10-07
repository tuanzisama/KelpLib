package org.bukkit.entity;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

/**
 * Minimal compile-time stub of org.bukkit.entity.HumanEntity.
 */
public interface HumanEntity extends LivingEntity {

    PlayerInventory getInventory();

    void closeInventory();
}
