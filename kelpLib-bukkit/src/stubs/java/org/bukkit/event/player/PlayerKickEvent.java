package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Minimal compile-time stub of org.bukkit.event.player.PlayerKickEvent.
 */
public class PlayerKickEvent extends PlayerEvent implements org.bukkit.event.Cancellable {

    private boolean cancelled;

    public PlayerKickEvent(Player player) {
        super(player);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
