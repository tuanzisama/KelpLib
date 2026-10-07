package org.bukkit.event.server;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Minimal compile-time stub of org.bukkit.event.server.ServerEvent.
 */
public abstract class ServerEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
