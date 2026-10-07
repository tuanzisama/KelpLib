package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Minimal compile-time stub of org.bukkit.event.block.BlockBreakEvent.
 */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private boolean cancelled;

    public BlockBreakEvent(Block block, Player player) {
        super(block);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
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
