package org.bukkit;

import org.bukkit.plugin.Plugin;

/**
 * Minimal compile-time stub of org.bukkit.NamespacedKey.
 */
public final class NamespacedKey {

    private final String namespace;
    private final String key;

    public NamespacedKey(Plugin plugin, String key) {
        this.namespace = plugin.getName().toLowerCase(java.util.Locale.ROOT);
        this.key = key.toLowerCase(java.util.Locale.ROOT);
    }

    public NamespacedKey(String namespace, String key) {
        this.namespace = namespace.toLowerCase(java.util.Locale.ROOT);
        this.key = key.toLowerCase(java.util.Locale.ROOT);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return namespace + ":" + key;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NamespacedKey other)) {
            return false;
        }
        return namespace.equals(other.namespace) && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return namespace.hashCode() * 31 + key.hashCode();
    }
}
