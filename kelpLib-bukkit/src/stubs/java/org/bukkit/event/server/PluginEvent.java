package org.bukkit.event.server;

import org.bukkit.plugin.Plugin;

/**
 * Minimal compile-time stub of org.bukkit.event.server.PluginEvent.
 */
public abstract class PluginEvent extends ServerEvent {

    private final Plugin plugin;

    protected PluginEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public final Plugin getPlugin() {
        return plugin;
    }
}
