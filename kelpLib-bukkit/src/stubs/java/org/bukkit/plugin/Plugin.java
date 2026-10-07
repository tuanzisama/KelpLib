package org.bukkit.plugin;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Server;

/**
 * Minimal compile-time stub of org.bukkit.plugin.Plugin.
 */
public interface Plugin {

    String getName();

    File getDataFolder();

    Logger getLogger();

    Server getServer();

    boolean isEnabled();
}
