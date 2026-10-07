package org.bukkit.plugin.messaging;

import org.bukkit.plugin.Plugin;

/**
 * Minimal compile-time stub of org.bukkit.plugin.messaging.Messenger.
 */
public interface Messenger {

    void registerOutgoingPluginChannel(Plugin plugin, String channel);

    void unregisterOutgoingPluginChannel(Plugin plugin, String channel);

    void registerIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener);

    void unregisterIncomingPluginChannel(Plugin plugin, String channel);

    void unregisterIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener);
}
