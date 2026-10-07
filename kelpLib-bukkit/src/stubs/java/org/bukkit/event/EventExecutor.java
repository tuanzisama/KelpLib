package org.bukkit.event;

/**
 * Minimal compile-time stub of org.bukkit.event.EventExecutor.
 */
@FunctionalInterface
public interface EventExecutor {

    void execute(Listener listener, Event event) throws EventException;
}
