package org.bukkit.plugin.java;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Minimal compile-time stub of org.bukkit.plugin.java.JavaPlugin.
 */
public abstract class JavaPlugin implements Plugin {

    protected JavaPlugin() {
    }

    @Override
    public final String getName() {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public final File getDataFolder() {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public final Logger getLogger() {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public final Server getServer() {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public final boolean isEnabled() {
        throw new UnsupportedOperationException("stub");
    }

    public void onLoad() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }
}
