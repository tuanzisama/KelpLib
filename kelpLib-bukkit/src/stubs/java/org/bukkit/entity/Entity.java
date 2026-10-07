package org.bukkit.entity;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Minimal compile-time stub of org.bukkit.entity.Entity.
 */
public interface Entity {

    UUID getUniqueId();

    String getName();

    Location getLocation();

    World getWorld();

    boolean isDead();

    EntityScheduler getScheduler();
}
