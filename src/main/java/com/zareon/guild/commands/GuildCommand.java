package com.zareon.guild.commands;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import com.zareon.guild.models.GuildRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class GuildCommand implements CommandExecutor, TabCompleter {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public GuildCommand(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.player-only")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "leave" -> handleLeave(player);
            case "members" -> handleMembers(player);
            case "disband" -> handleDisband(player);
            case "rename" -> handleRename(player, args);
            case "menu" -> handleMenu(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.< 2) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.create.usage")));
            return;
        }

        if (!player.hasPermission("zareonguild.create")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.no-permission")));
            return;
        }

        Optional<Guild> existingGuild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (existingGuild.isPresent()) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.create.already-in-guild")));
            return;
        }

        String guildName = args[1];
        if (plugin.getGuildManager().getGuildByName(guildName).isPresent()) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.create.name-taken")));
            return;
        }

        double cost = plugin.getConfigManager().getCreateCost();
        if (cost > 0 && !plugin.getEconomyManager().withdrawPlayer(player, cost)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("commands.create.insufficient-funds")
                            .replace("{cost}", String.valueOf(cost))
            ));
            return;
        }

        Guild guild = plugin.getGuildManager().createGuild(player, guildName);
        if (guild != null) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("commands.create.success")
                            .replace("{guild}", guildName)
            ));
        } else {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.create.failed")));
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.invite.usage")));
            return;
        }

        Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guildOpt.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
            return;
        }

        Guild guild = guildOpt.get();
        GuildMember member = guild.getMember(player.getUniqueId());
        if (!member.getRank().hasPermission("invite")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.invite.no-permission")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.invite.player-not-found")));
            return;
        }

        if (plugin.getGuildManager().getGuildByPlayer(target.getUniqueId()).isPresent()) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.invite.already-in-guild")));
            return;
        }

        plugin.getGuildManager().addInvite(target.getUniqueId(), guild.getName());

        // Send clickable invite to target
        Component inviteMessage = miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.invite.received")
                        .replace("{guild}", guild.getName())
                        .replace("{player}", player.getName())
        );

        Component acceptButton = miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.invite.accept-button")
        ).clickEvent(ClickEvent.runCommand("/guild accept"));

        target.sendMessage(inviteMessage);
        target.sendMessage(acceptButton);

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.invite.sent")
                        .replace("{player}", target.getName())
        ));
    }

    private void handleAccept(Player player) {
        String guildName = plugin.getGuildManager().getInvite(player.getUniqueId());
        if (guildName == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.accept.no-invite")));
            return;
        }

        Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByName(guildName);
        if (guildOpt.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.accept.guild-not-found")));
            plugin.getGuildManager().removeInvite(player.getUniqueId());
            return;
        }

        Guild guild = guildOpt.get();
        guild.addMember(player.getUniqueId(), plugin.getRankManager().getRank("BEGINNER"));
        plugin.getGuildManager().removeInvite(player.getUniqueId());

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.accept.success")
                        .replace("{guild}", guild.getName())
        ));

        // Notify guild members
        guild.getMembers().forEach(memberUUID -> {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getMessage("commands.accept.member-joined")
                                .replace("{player}", player.getName())
                ));
            }
        });
    }

    private void handleLeave(Player player) {
        Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guildOpt.isEmpty())miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
        return;
    }

    Guild guild = guildOpt.get();
        if (guild.getLeader().equals(player.getUniqueId())) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.leave.leader-cannot-leave")));
        return;
    }

        guild.removeMember(player.getUniqueId());
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.leave.success")
                .replace("{guild}", guild.getName())
            ));
}

private void handleMembers(Player player) {
    Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
    if (guildOpt.isEmpty()) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
        return;
    }

    Guild guild = guildOpt.get();
    player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getMessage("commands.members.header")
                    .replace("{guild}", guild.getName())
    ));

    guild.getMembers().forEach(memberUUID -> {
        GuildMember member = guild.getMember(memberUUID);
        String memberName = Bukkit.getOfflinePlayer(memberUUID).getName();
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.members.format")
                        .replace("{player}", memberName)
                        .replace("{rank}", member.getRank().getName())
        ));
    });
}

private void handleDisband(Player player) {
    Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
    if (guildOpt.isEmpty()) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
        return;
    }

    Guild guild = guildOpt.get();
    if (!guild.getLeader().equals(player.getUniqueId())) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.disband.not-leader")));
        return;
    }

    String guildName = guild.getName();
    plugin.getGuildManager().deleteGuild(guild);
    player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getMessage("commands.disband.success")
                    .replace("{guild}", guildName)
    ));
}

private void handleRename(Player player, String[] args) {
    if (args.length < 2) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.rename.usage")));
        return;
    }

    Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
    if (guildOpt.isEmpty()) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
        return;
    }

    Guild guild = guildOpt.get();
    GuildMember member = guild.getMember(player.getUniqueId());
    if (!member.getRank().hasPermission("rename")) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.rename.no-permission")));
        return;
    }

    String newName = args[1];
    if (plugin.getGuildManager().getGuildByName(newName).isPresent()) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.rename.name-taken")));
        return;
    }

    double cost = plugin.getConfigManager().getRenameCost();
    if (cost > 0 && !plugin.getEconomyManager().withdrawPlayer(player, cost)) {
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("commands.rename.insufficient-funds")
                        .replace("{cost}", String.valueOf(cost))
        ));
        return;
    }

    String oldName = guild.getName();
    guild.setName(newName);
    player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getMessage("commands.rename.success")
                    .replace("{old}", oldName)
                    .replace("{new}", newName)
    ));
}

private void handleMenu(Player player) {
    Optional<Guild> guildOpt = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
    if (guildOpt.isEmpty()) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("commands.not-in-guild")));
        return;
    }

    plugin.getGuiManager().openMainMenu(player);
}

private void sendHelp(Player player) {
    List<String> helpMessages = plugin.getConfigManager().getHelpMessages();
    helpMessages.forEach(msg -> player.sendMessage(miniMessage.deserialize(msg)));
}

@Override
public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
        return Arrays.asList("create", "invite", "accept", "leave", "members", "disband", "rename", "menu");
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
    }

    return new ArrayList<>();
}
}
