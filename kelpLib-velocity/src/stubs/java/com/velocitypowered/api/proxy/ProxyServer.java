package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.scheduler.Scheduler;

import java.util.Collection;
import java.util.Optional;

/**
 * Minimal compile-time stub of com.velocitypowered.api.proxy.ProxyServer.
 */
public interface ProxyServer {

    Scheduler getScheduler();

    CommandManager getCommandManager();

    EventManager getEventManager();

    PluginManager getPluginManager();

    ChannelRegistrar getChannelRegistrar();

    Optional<Player> getPlayer(String username);

    Optional<Player> getPlayer(java.util.UUID uuid);

    Collection<Player> getAllPlayers();

    int getPlayerCount();

    ConsoleCommandSource getConsoleCommandSource();
}
