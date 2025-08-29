package com.zareon.guild.listener;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.manager.GuildManager;
import com.zareon.guild.manager.TNTManager;
import com.zareon.guild.util.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftListeners implements Listener {

    private final ZareonGuildPlugin plugin;
    private final MessageService msg;
    private final GuildManager guilds;
    private final TNTManager tnt;

    public CraftListeners(ZareonGuildPlugin plugin, MessageService msg, GuildManager guilds, TNTManager tnt) {
        this.plugin = plugin; this.msg = msg; this.guilds = guilds; this.tnt = tnt;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        if (!(e.getView().getPlayer() instanceof Player player)) return;
        ItemStack result = inv.getResult();
        if (result == null) return;

        if (result.getType() == org.bukkit.Material.TNT && tnt.isGuildTNT(result)) {
            Guild g = guilds.getGuildByPlayer(player.getUniqueId());
            int required = tnt.requiredLevel(result);
            if (g == null || g.getLevel() < required) {
                inv.setResult(null);
                msg.send(player, "tnt.not_allowed_level");
            }
        }
    }
}
