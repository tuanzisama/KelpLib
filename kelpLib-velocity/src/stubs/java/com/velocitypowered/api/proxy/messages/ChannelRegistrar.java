package com.velocitypowered.api.proxy.messages;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.messages.ChannelRegistrar.
 */
public interface ChannelRegistrar {

    void register(ChannelIdentifier... identifiers);

    void unregister(ChannelIdentifier... identifiers);
}
