package org.bukkit.plugin.messaging;

import org.bukkit.entity.Player;

/**
 * Minimal compile-time stub of org.bukkit.plugin.messaging.PluginMessageListener.
 */
public interface PluginMessageListener {

    void pluginMessageReceived(String channel, Player player, byte[] message);
}
