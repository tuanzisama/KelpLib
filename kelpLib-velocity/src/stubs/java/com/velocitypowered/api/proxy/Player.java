package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.PlayerSettings;

import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.Player.
 */
public interface Player extends CommandSource, ChannelMessageSink {

    String getUsername();

    UUID getUniqueId();

    Optional<ServerConnection> getCurrentServer();

    PlayerSettings getSettings();

    void disconnect(Component reason);

    default Optional<ServerInfo> getCurrentServerInfo() {
        return getCurrentServer().map(ServerConnection::getServerInfo);
    }
}
