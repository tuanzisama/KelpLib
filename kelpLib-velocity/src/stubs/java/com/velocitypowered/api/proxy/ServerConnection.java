package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.ServerConnection.
 */
public interface ServerConnection extends ChannelMessageSink {

    ServerInfo getServerInfo();

    boolean isActive();
}
