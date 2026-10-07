package org.bukkit.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal compile-time stub of org.bukkit.event.HandlerList.
 */
public final class HandlerList {

    private final List<RegisteredListener> listeners = new ArrayList<>();

    public HandlerList() {
    }

    public synchronized void register(RegisteredListener listener) {
        listeners.add(listener);
    }

    public synchronized void unregister(RegisteredListener listener) {
        listeners.remove(listener);
    }

    public synchronized void unregister(org.bukkit.plugin.Plugin plugin) {
        listeners.removeIf(registered -> registered.getPlugin() == plugin);
    }

    public synchronized void unregister(Listener listener) {
        listeners.removeIf(registered -> registered.getListener() == listener);
    }

    public static void unregisterAll(Listener listener) {
        // stub：真实实现按插件注销全部监听
    }

    public static void unregisterAll(org.bukkit.plugin.Plugin plugin) {
        // stub
    }

    public synchronized List<RegisteredListener> getRegisteredListeners() {
        return List.copyOf(listeners);
    }
}
