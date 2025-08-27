package com.zareon.guild.placeholders;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuildPlaceholders extends PlaceholderExpansion {
    private final ZareonGuild plugin;

    public GuildPlaceholders(ZareonGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zareonguild";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Zareon";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // Get player's guild
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());

        if (guild == null) {
            // No guild
            if (identifier.equals("tag")) {
                return "";
            } else if (identifier.equals("rank")) {
                return "";
            }
            return "";
        }

        // %zareonguild_tag%
        if (identifier.equals("tag")) {
            return guild.getTag();
        }

        // %zareonguild_rank%
        if (identifier.equals("rank")) {
            GuildMember member = guild.getMember(player.getUniqueId());
            if (member != null) {
                return plugin.getRankManager().getRankDisplay(member.getRank());
            }
            return "";
        }

        return null;
    }
}
