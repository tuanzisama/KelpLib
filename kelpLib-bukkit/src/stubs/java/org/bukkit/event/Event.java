package org.bukkit.event;

/**
 * Minimal compile-time stub of org.bukkit.event.Event.
 */
public abstract class Event {

    private final String name = getClass().getSimpleName();

    public String getEventName() {
        return name;
    }

    public abstract HandlerList getHandlers();

    public boolean isAsynchronous() {
        return false;
    }
}
