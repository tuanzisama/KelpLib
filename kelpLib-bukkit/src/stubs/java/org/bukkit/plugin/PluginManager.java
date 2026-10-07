package org.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;

/**
 * Minimal compile-time stub of org.bukkit.plugin.PluginManager.
 */
public interface PluginManager {

    void registerEvents(Listener listener, Plugin service);

    void callEvent(Event event);

    Plugin getPlugin(String name);

    boolean isPluginEnabled(String name);
}
