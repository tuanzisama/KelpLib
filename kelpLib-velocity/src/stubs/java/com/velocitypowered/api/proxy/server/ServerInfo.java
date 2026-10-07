package com.velocitypowered.api.proxy.server;

import java.net.InetSocketAddress;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.server.ServerInfo.
 */
public final class ServerInfo {

    private final String name;
    private final InetSocketAddress address;

    public ServerInfo(String name, InetSocketAddress address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return name;
    }
}
