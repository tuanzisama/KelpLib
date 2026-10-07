package org.bukkit.inventory;

import org.bukkit.Material;

/**
 * Minimal compile-time stub of org.bukkit.inventory.ItemStack.
 */
public class ItemStack implements Cloneable {

    private Material type;
    private int amount;

    public ItemStack(Material type) {
        this(type, 1);
    }

    public ItemStack(Material type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public ItemMeta getItemMeta() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean setItemMeta(ItemMeta itemMeta) {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public ItemStack clone() {
        try {
            return (ItemStack) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemStack other && other.type == type && other.amount == amount;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + amount;
    }
}
