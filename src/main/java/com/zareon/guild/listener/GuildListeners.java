package com.zareon.guild.listener;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.manager.GuildManager;
import com.zareon.guild.util.MessageService;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class GuildListeners implements Listener {

    private final ZareonGuildPlugin plugin;
    private final MessageService msg;
    private final GuildManager guilds;

    public GuildListeners(ZareonGuildPlugin plugin, MessageService msg, GuildManager guilds) {
        this.plugin = plugin; this.msg = msg; this.guilds = guilds;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        plugin.gui().handleClick(e);
    }

    public void sendInvite(Player inviter, Player target, Guild guild) {
        // Notify inviter
        msg.send(inviter, "guild.invite_sent", Map.of("player", target.getName()));

        // Invite target with clickable ACCEPT
        var line1 = msg.format("guild.invited.line1", Map.of("name", guild.getName()));
        var button = msg.format("guild.invited.button", Map.of())
                .hoverEvent(HoverEvent.showText(msg.format("guild.invited.hover", Map.of())))
                .clickEvent(ClickEvent.runCommand("/guild accept"));
        target.sendMessage(msg.prefix().append(line1));
        target.sendMessage(button);
    }
}
