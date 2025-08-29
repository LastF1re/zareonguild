package com.zareon.guild.hook;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.data.Rank;
import com.zareon.guild.manager.GuildManager;
import com.zareon.guild.manager.RankManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiExpansion extends PlaceholderExpansion {

    private final ZareonGuildPlugin plugin;
    private final GuildManager guilds;
    private final RankManager ranks;

    public PapiExpansion(ZareonGuildPlugin plugin, GuildManager guilds, RankManager ranks) {
        this.plugin = plugin;
        this.guilds = guilds;
        this.ranks = ranks;
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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        Guild g = guilds.getGuildByPlayer(player.getUniqueId());
        switch (params.toLowerCase()) {
            case "tag":
                return g != null ? g.getTag() : plugin.messages().apply(plugin.getConfig().getString("placeholders.no_guild_tag", "<gray>-</gray>"), null);
            case "rank":
                Rank r = guilds.getRank(player.getUniqueId());
                return r != null ? r.name() : plugin.getConfig().getString("placeholders.no_rank", "-");
            default:
                return "";
        }
    }
}
