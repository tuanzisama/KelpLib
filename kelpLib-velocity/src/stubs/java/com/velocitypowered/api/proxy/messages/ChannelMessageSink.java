package com.velocitypowered.api.proxy.messages;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.messages.ChannelMessageSink.
 */
public interface ChannelMessageSink {

    boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data);
}
