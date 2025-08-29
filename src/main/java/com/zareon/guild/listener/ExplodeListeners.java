package com.zareon.guild.listener;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.manager.GuildManager;
import com.zareon.guild.manager.RegionManager;
import com.zareon.guild.manager.TNTManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashSet;
import java.util.Set;

public class ExplodeListeners implements Listener {

    private final ZareonGuildPlugin plugin;
    private final GuildManager guilds;
    private final TNTManager tnt;
    private final RegionManager regions;

    public ExplodeListeners(ZareonGuildPlugin plugin, GuildManager guilds, TNTManager tnt, RegionManager regions) {
        this.plugin = plugin; this.guilds = guilds; this.tnt = tnt; this.regions = regions;
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed primed)) return;
        // Attempt to detect if this was Guild TNT by checking source item power name
        // We cannot get the original item reliably; use explosion manipulation broadly
        double yield = 1.0;
        e.setYield((float) yield);

        // Allow breaking obsidian and water within radius
        Set<Block> extra = new HashSet<>();
        int r = 3;
        var loc = e.getLocation();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                    Material m = b.getType();
                    if (m == Material.OBSIDIAN || m == Material.CRYING_OBSIDIAN || m == Material.WATER || m == Material.LAVA) {
                        extra.add(b);
                    }
                }
            }
        }
        e.blockList().addAll(extra);
    }
}
