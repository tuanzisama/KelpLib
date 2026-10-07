package org.bukkit.command;

import java.util.List;

/**
 * Minimal compile-time stub of org.bukkit.command.Command.
 */
public abstract class Command {

    private final String name;
    private String permission;
    private String description = "";
    private String usage = "";

    protected Command(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean setPermission(String permission) {
        this.permission = permission;
        return true;
    }

    public String getPermission() {
        return permission;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getUsage() {
        return usage;
    }

    public abstract boolean execute(CommandSender sender, String label, String[] args);

    public boolean unregister(org.bukkit.command.CommandMap map) {
        return false;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
