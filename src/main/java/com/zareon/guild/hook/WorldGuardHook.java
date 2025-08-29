package com.zareon.guild.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location; // WorldEdit Location
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer; // Изменено здесь
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;

import java.util.UUID;


public class WorldGuardHook {
    private final boolean present;

    public WorldGuardHook(Object plugin) {
        this.present = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public boolean isPresent() {
        return present;
    }

    /**
     * Создаёт кубоидный регион с указанным владельцем
     */
    public boolean createCuboidRegion(String id, org.bukkit.Location center, int radius, UUID owner) {
        if (!present) return false;

        World wgWorld = BukkitAdapter.adapt(center.getWorld());
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgWorld);
        if (rm == null) return false;

        BlockVector3 min = BlockVector3.at(
                center.getBlockX() - radius,
                center.getWorld().getMinHeight(),
                center.getBlockZ() - radius
        );
        BlockVector3 max = BlockVector3.at(
                center.getBlockX() + radius,
                center.getWorld().getMaxHeight(),
                center.getBlockZ() + radius
        );

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(id, min, max);
        region.getOwners().addPlayer(owner);

        try {
            rm.addRegion(region);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверяет, пересекается ли кубоид с существующими регионами
     */
    public boolean overlaps(org.bukkit.Location center, int radius) {
        if (!present) return false;

        World wgWorld = BukkitAdapter.adapt(center.getWorld());
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rm = container.get(wgWorld);
        if (rm == null) return false;

        BlockVector3 min = BlockVector3.at(
                center.getBlockX() - radius,
                center.getWorld().getMinHeight(),
                center.getBlockZ() - radius
        );
        BlockVector3 max = BlockVector3.at(
                center.getBlockX() + radius,
                center.getWorld().getMaxHeight(),
                center.getBlockZ() + radius
        );

        RegionQuery query = container.createQuery();

        // Проверяем только углы кубоида для ускорения
        BlockVector3[] corners = new BlockVector3[]{
                BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
                BlockVector3.at(min.getBlockX(), min.getBlockY(), max.getBlockZ()),
                BlockVector3.at(max.getBlockX(), min.getBlockY(), min.getBlockZ()),
                BlockVector3.at(max.getBlockX(), min.getBlockY(), max.getBlockZ()),
                BlockVector3.at(min.getBlockX(), max.getBlockY(), min.getBlockZ()),
                BlockVector3.at(min.getBlockX(), max.getBlockY(), max.getBlockZ()),
                BlockVector3.at(max.getBlockX(), max.getBlockY(), min.getBlockZ()),
                BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ())
        };

        for (BlockVector3 corner : corners) {
            // Конвертируем в WorldEdit Location
            Location weLoc = new Location(wgWorld, corner.getX(), corner.getY(), corner.getZ());
            ApplicableRegionSet set = query.getApplicableRegions(weLoc);
            if (!set.getRegions().isEmpty()) {
                return true; // есть пересечение
            }
        }

        return false;
    }
}
