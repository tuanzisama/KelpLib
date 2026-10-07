package org.bukkit.event;

import org.bukkit.plugin.Plugin;

/**
 * Minimal compile-time stub of org.bukkit.event.RegisteredListener.
 */
public class RegisteredListener {

    private final Listener listener;
    private final EventExecutor executor;
    private final EventPriority priority;
    private final Plugin plugin;
    private final boolean ignoreCancelled;

    public RegisteredListener(Listener listener, EventExecutor executor, EventPriority priority,
                              Plugin plugin, boolean ignoreCancelled) {
        this.listener = listener;
        this.executor = executor;
        this.priority = priority;
        this.plugin = plugin;
        this.ignoreCancelled = ignoreCancelled;
    }

    public Listener getListener() {
        return listener;
    }

    public EventExecutor getExecutor() {
        return executor;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    public void callEvent(Event event) throws EventException {
        executor.execute(listener, event);
    }
}
