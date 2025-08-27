package com.zareon.guild.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildRegion implements ConfigurationSerializable {
    private UUID id;
    private Location center;
    private int radius;
    private int height;
    private String type; // SMALL, MEDIUM, LARGE
    private long createdTime;

    public GuildRegion(Location center, int radius, int height, String type) {
        this.id = UUID.randomUUID();
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.type = type;
        this.createdTime = System.currentTimeMillis();
    }

    public GuildRegion(Map<String, Object> map) {
        this.id = UUID.fromString((String) map.get("id"));

        // Deserialize location
        String worldName = (String) map.get("world");
        double x = (double) map.get("x");
        double y = (double) map.get("y");
        double z = (double) map.get("z");
        World world = Bukkit.getWorld(worldName);
        this.center = new Location(world, x, y, z);

        this.radius = (int) map.get("radius");
        this.height = (int) map.get("height");
        this.type = (String) map.get("type");
        this.createdTime = (long) map.get("createdTime");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("world", center.getWorld().getName());
        map.put("x", center.getX());
        map.put("y", center.getY());
        map.put("z", center.getZ());
        map.put("radius", radius);
        map.put("height", height);
        map.put("type", type);
        map.put("createdTime", createdTime);
        return map;
    }

    public boolean isInRegion(Location location) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }

        double distanceSquared = location.distanceSquared(center);
        double radiusSquared = radius * radius;

        boolean isWithinRadius = distanceSquared <= radiusSquared;
        boolean isWithinHeight = Math.abs(location.getY() - center.getY()) <= height / 2.0;

        return isWithinRadius && isWithinHeight;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Location getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public int getHeight() {
        return height;
    }

    public String getType() {
        return type;
    }

    public long getCreatedTime() {
        return createdTime;
    }
}
