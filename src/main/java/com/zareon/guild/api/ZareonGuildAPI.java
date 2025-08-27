package com.zareon.guild.api;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import com.zardRank;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ZareonGuildAPI {
    private static ZareonGuild plugin;

    public static void setPlugin(ZareonGuild plugin) {
        ZareonGuildAPI.plugin = plugin;
    }

    /**
     * Get guild by player UUID
     * @param playerUUID Player's UUID
     * @return Optional guild
     */
    public static Optional<Guild> getGuildByPlayer(UUID playerUUID) {
        return plugin.getGuildManager().getGuildByPlayer(playerUUID);
    }

    /**
     * Get guild by name
     * @param name Guild name
     * @return Optional guild
     */
    public static Optional<Guild> getGuildByName(String name) {
        return plugin.getGuildManager().getGuildByName(name);
    }

    /**
     * Get player's guild rank
     * @param playerUUID Player's UUID
     * @return Guild rank or null if not in guild
     */
    public static GuildRank getPlayerRank(UUID playerUUID) {
        Optional<Guild> guild = getGuildByPlayer(playerUUID);
        if (guild.isPresent()) {
            GuildMember member = guild.get().getMember(playerUUID);
            return member != null ? member.getRank() : null;
        }
        return null;
    }

    /**
     * Get guild tag/prefix for player
     * @param playerUUID Player's UUID
     * @return Guild tag or empty string
     */
    public static String getGuildTag(UUID playerUUID) {
        Optional<Guild> guild = getGuildByPlayer(playerUUID);
        return guild.map(Guild::getTag).orElse("");
    }

    /**
     * Get player's rank name
     * @param playerUUID Player's UUID
     * @return Rank name or empty string
     */
    public static String getPlayerRankName(UUID playerUUID) {
        GuildRank rank = getPlayerRank(playerUUID);
        return rank != null ? rank.getName() : "";
    }

    /**
     * Check if player has permission in guild
     * @param playerUUID Player's UUID
     * @param permission Permission to check
     * @return True if has permission
     */
    public static boolean hasGuildPermission(UUID playerUUID, String permission) {
        GuildRank rank = getPlayerRank(playerUUID);
        return rank != null && rank.hasPermission(permission);
    }

    /**
     * Get all guilds
     * @return List of all guilds
     */
    public static List<Guild> getAllGuilds() {
        return plugin.getGuildManager().getAllGuilds();
    }

    /**
     * Create a new guild
     * @param leader Guild leader
     * @param name Guild name
     * @return Created guild or null if failed
     */
    public static Guild createGuild(Player leader, String name) {
        return plugin.getGuildManager().createGuild(leader, name);
    }

    /**
     * Delete guild
     * @param guild Guild to delete
     * @return True if successful
     */
    public static boolean deleteGuild(Guild guild) {
        return plugin.getGuildManager().deleteGuild(guild);
    }

    /**
     * Check if player can use private block
     * @param playerUUID Player's UUID
     * @param blockLevel Required block level
     * @return True if can use
     */
    public static boolean canUsePrivateBlock(UUID playerUUID, int blockLevel) {
        Optional<Guild> guild = getGuildByPlayer(playerUUID);
        return guild.map(g -> g.getLevel() >= blockLevel).orElse(false);
    }

    /**
     * Check if player can craft TNT
     * @param playerUUID Player's UUID
     * @param tntLevel Required TNT level
     * @return True if can craft
     */
    public static boolean canCraftTNT(UUID playerUUID, int tntLevel) {
        Optional<Guild> guild = getGuildByPlayer(playerUUID);
        if (!guild.isPresent()) return false;

        int requiredGuildLevel = switch (tntLevel) {
            case 1 -> 2; // Small TNT requires guild level 2
            case 2 -> 3; // Medium TNT requires guild level 3
            case 3 -> 5; // Large TNT requires guild level 5
            default -> Integer.MAX_VALUE;
        };

        return guild.get().getLevel() >= requiredGuildLevel;
    }
}
