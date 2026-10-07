package org.bukkit.persistence;

import org.bukkit.NamespacedKey;

import java.util.Set;

/**
 * Minimal compile-time stub of org.bukkit.persistence.PersistentDataContainer.
 */
public interface PersistentDataContainer {

    <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value);

    <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type);

    <T, Z> boolean has(NamespacedKey key, PersistentDataType<T, Z> type);

    void remove(NamespacedKey key);

    Set<NamespacedKey> getKeys();
}
