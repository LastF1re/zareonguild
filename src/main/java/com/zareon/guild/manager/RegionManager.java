package com.zareon.guild.manager;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.hook.WorldGuardHook;
import com.zareon.guild.region.CustomRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegionManager {
    private final ZareonGuildPlugin plugin;
    private final WorldGuardHook wg;

    private final List<CustomRegion> customRegions = new ArrayList<>();

    public RegionManager(ZareonGuildPlugin plugin, WorldGuardHook wg) {
        this.plugin = plugin;
        this.wg = wg;
    }

    public boolean overlaps(Location center, int radius) {
        if (wg.isPresent() && wg.overlaps(center, radius)) return true;
        for (CustomRegion r : customRegions) {
            if (r.overlaps(center, radius)) return true;
        }
        return false;
    }

    public boolean createRegion(Guild guild, Location center, int radius) {
        String id = "zareon_" + guild.getName().toLowerCase() + "_" + center.getBlockX() + "_" + center.getBlockZ();
        if (wg.isPresent()) {
            if (wg.overlaps(center, radius)) return false;
            boolean ok = wg.createCuboidRegion(id, center, radius, guild.getLeader());
            return ok;
        } else {
            CustomRegion region = new CustomRegion(guild.getName(), center, radius);
            if (overlaps(center, radius)) return false;
            customRegions.add(region);
            guild.getRegions().add(region);
            return true;
        }
    }

    public List<CustomRegion> getRegions(Guild g) {
        return g.getRegions();
    }
}
