package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Minimal compile-time stub of org.bukkit.event.block.BlockEvent.
 */
public abstract class BlockEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Block block;

    protected BlockEvent(Block block) {
        this.block = block;
    }

    public final Block getBlock() {
        return block;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
