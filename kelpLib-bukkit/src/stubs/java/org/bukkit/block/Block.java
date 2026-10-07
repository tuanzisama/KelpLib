package org.bukkit.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Minimal compile-time stub of org.bukkit.block.Block.
 */
public interface Block {

    Location getLocation();

    Material getType();

    World getWorld();

    int getX();

    int getY();

    int getZ();
}
