package com.zareon.guild.region;

import org.bukkit.Location;

public class CustomRegion {
    private final String guildName;
    private final Location center;
    private final int radius;

    public CustomRegion(String guildName, Location center, int radius) {
        this.guildName = guildName;
        this.center = center.clone();
        this.radius = radius;
    }

    public String getGuildName() { return guildName; }
    public Location getCenter() { return center.clone(); }
    public int getRadius() { return radius; }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return Math.abs(loc.getBlockX() - center.getBlockX()) <= radius
                && Math.abs(loc.getBlockZ() - center.getBlockZ()) <= radius;
    }

    public boolean overlaps(Location otherCenter, int otherRadius) {
        if (!otherCenter.getWorld().equals(center.getWorld())) return false;
        int dx = Math.abs(otherCenter.getBlockX() - center.getBlockX());
        int dz = Math.abs(otherCenter.getBlockZ() - center.getBlockZ());
        return dx <= (radius + otherRadius) && dz <= (radius + otherRadius);
    }
}
