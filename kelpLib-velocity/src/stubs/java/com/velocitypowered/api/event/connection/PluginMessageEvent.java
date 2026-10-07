package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;

/**
 * Minimal compile-time stub of com.velocitypowered.api.event.connection.PluginMessageEvent.
 */
public final class PluginMessageEvent {

    private final Object source;
    private final ChannelIdentifier identifier;
    private final byte[] data;

    public PluginMessageEvent(Object source, ChannelIdentifier identifier, byte[] data) {
        this.source = source;
        this.identifier = identifier;
        this.data = data;
    }

    public Object getSource() {
        return source;
    }

    public ChannelIdentifier getIdentifier() {
        return identifier;
    }

    public byte[] getData() {
        return data;
    }
}
