package org.bukkit.inventory;

import net.kyori.adventure.text.Component;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;

/**
 * Minimal compile-time stub of org.bukkit.inventory.ItemMeta（Paper Adventure 方法以语句调用，返回值不依赖）。
 */
public interface ItemMeta {

    void displayName(Component displayName);

    void lore(List<Component> lore);

    boolean addEnchant(Enchantment ench, int level, boolean ignoreLevelRestrictions);

    void setUnbreakable(boolean unbreakable);

    PersistentDataContainer getPersistentDataContainer();
}
