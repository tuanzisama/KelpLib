package com.velocitypowered.api.plugin;

import java.util.Optional;

/**
 * Minimal compile-time stub of com.velocitypowered.api.plugin.PluginManager.
 */
public interface PluginManager {

    Optional<PluginContainer> getPlugin(String id);

    boolean isLoaded(String id);
}
