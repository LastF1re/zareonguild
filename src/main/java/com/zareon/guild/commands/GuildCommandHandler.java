package com.zareon.guild.commands;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuildCommandHandler implements CommandExecutor, TabCompleter {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GuildCommandHandler(ZareonGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Show help menu
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "members":
                handleMembers(player);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "rename":
                handleRename(player, args);
                break;
            case "menu":
                handleMenu(player);
                break;
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "Usage: /guild create <name>"));
            return;
        }

        if (!player.hasPermission("zareonguild.create")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        String guildName = args[1];

        // Check if player is already in a guild
        if (plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.already-in-guild")));
            return;
        }

        // Check if guild name is valid
        if (!guildName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("creation.invalid-name")));
            return;
        }

        // Check if guild name is taken
        if (plugin.getGuildManager().getGuildByName(guildName) != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("creation.name-taken")));
            return;
        }

        // Create guild
        Guild guild = plugin.getGuildManager().createGuild(guildName, player);

        if (guild != null) {
            String message = plugin.getConfigManager().getMessage("creation.success")
                    .replace("<name>", guildName);
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + message));
        } else {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.insufficient-funds")));
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "Usage: /guild invite <player>"));
            return;
        }

        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Check if player has permission to invite
        if (!guild.hasPermission(player.getUniqueId(), "INVITE_MEMBERS")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.insufficient-rank")));
            return;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.player-not-found")));
            return;
        }

        // Check if target is already in a guild
        if (plugin.getGuildManager().getGuildByPlayer(target.getUniqueId()) != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "That player is already in a guild!"));
            return;
        }

        // Send invitation
        boolean success = plugin.getGuildManager().invitePlayer(guild.getId(), player.getUniqueId(), target.getUniqueId());

        if (success) {
            // Notify inviter
            String inviterMessage = plugin.getConfigManager().getMessage("invitation.sent")
                    .replace("<player>", target.getName());
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + inviterMessage));

            // Notify target
            String targetMessage = plugin.getConfigManager().getMessage("invitation.received")
                    .replace("<player>", player.getName())
                    .replace("<guild>", guild.getName());

            target.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + targetMessage));
        }
    }

    private void handleAccept(Player player) {
        // Check if player has an invitation
        if (!plugin.getGuildManager().hasInvitation(player.getUniqueId())) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "You don't have any guild invitations!"));
            return;
        }

        // Accept invitation
        Guild guild = plugin.getGuildManager().getInvitationGuild(player.getUniqueId());
        boolean success = plugin.getGuildManager().acceptInvitation(player.getUniqueId());

        if (success) {
            String message = plugin.getConfigManager().getMessage("invitation.accepted")
                    .replace("<guild>", guild.getName());
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + message));

            // Notify other guild members
            for (UUID memberId : guild.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && !member.getUniqueId().equals(player.getUniqueId())) {
                    String joinMessage = plugin.getConfigManager().getMessage("members.joined")
                            .replace("<player>", player.getName());
                    member.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + joinMessage));
                }
            }
        }
    }

    private void handleLeave(Player player) {
        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Check if player is the leader
        if (guild.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() +
                    "As the leader, you cannot leave the guild. Use /guild disband or transfer leadership first."));
            return;
        }

        // Leave guild
        boolean success = plugin.getGuildManager().leaveGuild(player.getUniqueId());

        if (success) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "You have left the guild."));

            // Notify other guild members
            for (UUID memberId : guild.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    String leftMessage = plugin.getConfigManager().getMessage("members.left")
                            .replace("<player>", player.getName());
                    member.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + leftMessage));
                }
            }
        }
    }

    private void handleMembers(Player player) {
        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Show members
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "Guild Members:"));

        for (GuildMember member : guild.getMembers().values()) {
            String rankDisplay = plugin.getRankManager().getRankDisplay(member.getRank());
            String playerName = member.getPlayerName();

            if (guild.getLeader().equals(member.getPlayerId())) {
                playerName = playerName + " (Leader)";
            }

            player.sendMessage(miniMessage.deserialize("  " + rankDisplay + " " + playerName));
        }
    }

    private void handleDisband(Player player) {
        if (!player.hasPermission("zareonguild.disband")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Check if player is the leader
        if (!guild.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-leader")));
            return;
        }

        // Disband guild
        boolean success = plugin.getGuildManager().disbandGuild(guild.getId());

        if (success) {
            // Notify all members
            for (UUID memberId : guild.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("guild.disbanded")));
                }
            }
        }
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "Usage: /guild rename <new name>"));
            return;
        }

        if (!player.hasPermission("zareonguild.rename")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Check if player is the leader
        if (!guild.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-leader")));
            return;
        }

        String newName = args[1];

        // Check if guild name is valid
        if (!newName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("creation.invalid-name")));
            return;
        }

        // Check if guild name is taken
        if (plugin.getGuildManager().getGuildByName(newName) != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("creation.name-taken")));
            return;
        }

        // Check if player has enough money
        double renameCost = plugin.getConfigManager().getRenameCost();
        if (!plugin.getEconomyManager().hasEnough(player, renameCost)) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.insufficient-funds")));
            return;
        }

        // Charge the player
        plugin.getEconomyManager().withdraw(player, renameCost);

        // Rename guild
        String oldName = guild.getName();
        boolean success = plugin.getGuildManager().renameGuild(guild.getId(), newName);

        if (success) {
            String message = plugin.getConfigManager().getMessage("guild.renamed")
                    .replace("<old>", oldName)
                    .replace("<new>", newName);

            // Notify all members
            for (UUID memberId : guild.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + message));
                }
            }
        }
    }

    private void handleMenu(Player player) {
        // Check if player is in a guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("general.not-in-guild")));
            return;
        }

        // Open main menu
        plugin.getGuiManager().openMainMenu(player);
    }

    private void showHelp(Player player) {
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + "<gold>Guild Commands:</gold>"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild create <name></yellow> - Create a new guild"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild invite <player></yellow> - Invite a player to your guild"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild accept</yellow> - Accept a guild invitation"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild leave</yellow> - Leave your current guild"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild members</yellow> - View guild members"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild disband</yellow> - Disband your guild (leader only)"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild rename <name></yellow> - Rename your guild (leader only)"));
        player.sendMessage(miniMessage.deserialize("<yellow>/guild menu</yellow> - Open the guild menu"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("create", "invite", "accept", "leave", "members", "disband", "rename", "menu");
            return filterCompletions(subcommands, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite")) {
                return null; // Return null to get all online players
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String prefix) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
