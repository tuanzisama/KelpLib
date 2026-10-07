package com.velocitypowered.api.event;

/**
 * Minimal compile-time stub of com.velocitypowered.api.event.EventManager.
 */
public interface EventManager {

    void register(Object plugin, Object listener);

    void unregister(Object listener);
}
