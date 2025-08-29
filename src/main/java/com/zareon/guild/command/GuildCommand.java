package com.zareon.guild.command;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.manager.*;
import com.zareon.guild.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import com.zareon.guild.hook.EconomyManager;


import java.util.*;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private final ZareonGuildPlugin plugin;
    private final MessageService msg;
    private final GuildManager guilds;
    private final RankManager ranks;
    private final EconomyManager econ;
    private final GUIManager gui;

    public GuildCommand(ZareonGuildPlugin plugin, MessageService msg, GuildManager guilds, RankManager ranks,
                        EconomyManager econ, GUIManager gui) {
        this.plugin = plugin; this.msg = msg; this.guilds = guilds; this.ranks = ranks; this.econ = econ; this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.only_players");
            return true;
        }
        if (!player.hasPermission("zareonguild.command")) {
            msg.send(player, "general.no_permission");
            return true;
        }
        if (args.length == 0) {
            gui.openMain(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    msg.send(player, "general.unknown_subcommand");
                    return true;
                }
                if (guilds.getGuildByPlayer(player.getUniqueId()) != null) {
                    msg.send(player, "general.already_in_guild");
                    return true;
                }
                String name = args[1];
                if (!name.matches("^[A-Za-z0-9_]{3,16}$")) {
                    msg.send(player, "guild.invalid_name");
                    return true;
                }
                if (guilds.nameTaken(name)) {
                    msg.send(player, "guild.name_taken");
                    return true;
                }
                guilds.create(name, player.getUniqueId());
                msg.send(player, "guild.created", Map.of("name", name));
            }
            case "invite" -> {
                if (args.length < 2) {
                    msg.send(player, "general.unknown_subcommand");
                    return true;
                }
                Guild g = guilds.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    msg.send(player, "general.not_in_guild");
                    return true;
                }
                if (!plugin.ranks().has(g.getMember(player.getUniqueId()).get().getRank(), "invite")) {
                    msg.send(player, "general.no_permission");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    msg.send(player, "general.player_not_found");
                    return true;
                }
                guilds.addInvite(target.getUniqueId(), g.getName());
                plugin.getServer().getPluginManager().callEvent(new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/guild invite " + target.getName()));
                plugin.getServer().getPluginManager().registerEvents(new com.zareon.guild.listener.GuildListeners(plugin, msg, guilds), plugin);
                new com.zareon.guild.listener.GuildListeners(plugin, msg, guilds).sendInvite(player, target, g);
            }
            case "accept" -> {
                Optional<Guild> opt = guilds.consumeInvite(player.getUniqueId());
                if (opt.isEmpty()) {
                    msg.send(player, "general.guild_not_found");
                    return true;
                }
                Guild g = opt.get();
                if (!guilds.join(g, player.getUniqueId())) {
                    msg.send(player, "general.already_in_guild");
                    return true;
                }
                msg.send(player, "guild.accepted", Map.of("name", g.getName()));
            }
            case "leave" -> {
                if (!guilds.leave(player.getUniqueId())) {
                    msg.send(player, "general.not_in_guild");
                    return true;
                }
                msg.send(player, "guild.left", Map.of("name", guilds.getGuildByPlayer(player.getUniqueId()) != null ? guilds.getGuildByPlayer(player.getUniqueId()).getName() : ""));
            }
            case "members" -> {
                Guild g = guilds.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    msg.send(player, "general.not_in_guild");
                    return true;
                }
                player.sendMessage(msg.format("guild.member_list_title", Map.of()));
                for (var e : g.getMembers().entrySet()) {
                    OfflinePlayer op = guilds.offline(e.getKey());
                    player.sendMessage(msg.format("guild.member_line", Map.of("player", op.getName(), "rank", e.getValue().getRank().name())));
                }
            }
            case "disband" -> {
                Guild g = guilds.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    msg.send(player, "general.not_in_guild");
                    return true;
                }
                if (!g.isLeader(player.getUniqueId())) {
                    msg.send(player, "general.no_permission");
                    return true;
                }
                guilds.disband(g);
                msg.send(player, "guild.disbanded", Map.of("name", g.getName()));
            }
            case "rename" -> {
                if (args.length < 2) {
                    msg.send(player, "general.unknown_subcommand");
                    return true;
                }
                Guild g = guilds.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    msg.send(player, "general.not_in_guild");
                    return true;
                }
                if (!plugin.ranks().has(g.getMember(player.getUniqueId()).get().getRank(), "rename")) {
                    msg.send(player, "general.no_permission");
                    return true;
                }
                String newName = args[1];
                double cost = plugin.getConfig().getDouble("economy.rename_cost", 0.0);
                if (!econ.withdraw(player, cost)) {
                    msg.send(player, "levels.insufficient");
                    return true;
                }
                if (!guilds.rename(g, newName)) {
                    msg.send(player, "guild.name_taken");
                    return true;
                }
                msg.send(player, "guild.renamed", Map.of("name", newName));
            }
            case "menu" -> gui.openMain(player);
            default -> msg.send(player, "general.unknown_subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "invite", "accept", "leave", "members", "disband", "rename", "menu");
        }
        return Collections.emptyList();
    }
}
