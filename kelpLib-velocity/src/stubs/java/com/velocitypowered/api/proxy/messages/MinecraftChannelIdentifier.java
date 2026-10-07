package com.velocitypowered.api.proxy.messages;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.
 */
public final class MinecraftChannelIdentifier implements ChannelIdentifier {

    private final String namespace;
    private final String path;

    private MinecraftChannelIdentifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static MinecraftChannelIdentifier create(String namespace, String path) {
        return new MinecraftChannelIdentifier(namespace, path);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getId() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return getId();
    }
}
