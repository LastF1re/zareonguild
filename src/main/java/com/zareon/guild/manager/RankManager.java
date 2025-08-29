package com.zareon.guild.manager;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Rank;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RankManager {
    private final ZareonGuildPlugin plugin;
    private List<Rank> order = new ArrayList<>();
    private final Map<Rank, Map<String, Boolean>> perms = new HashMap<>();

    public RankManager(ZareonGuildPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        order.clear();
        perms.clear();
        for (String s : plugin.getConfig().getStringList("ranks.order")) {
            order.add(Rank.fromString(s));
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks.permissions");
        if (section != null) {
            for (String r : section.getKeys(false)) {
                Rank rank = Rank.fromString(r);
                Map<String, Boolean> map = new HashMap<>();
                for (String key : section.getConfigurationSection(r).getKeys(false)) {
                    map.put(key, section.getBoolean(r + "." + key));
                }
                perms.put(rank, map);
            }
        }
    }

    public List<Rank> order() { return Collections.unmodifiableList(order); }

    public boolean has(Rank rank, String perm) {
        return perms.getOrDefault(rank, Map.of()).getOrDefault(perm, false);
    }

    public boolean canPromote(Rank actor, Rank target) {
        if (!has(actor, "promote")) return false;
        return compare(actor, target) > 0 && target != Rank.LEADER;
    }

    public boolean canDemote(Rank actor, Rank target) {
        if (!has(actor, "demote")) return false;
        return compare(actor, target) > 0 && target != Rank.BEGINNER;
    }

    public int compare(Rank a, Rank b) {
        return Integer.compare(index(a), index(b));
    }

    public int index(Rank r) {
        int i = order.indexOf(r);
        return i == -1 ? 0 : i;
    }

    public Optional<Rank> next(Rank r) {
        int i = index(r);
        if (i + 1 < order.size()) return Optional.of(order.get(i + 1));
        return Optional.empty();
    }

    public Optional<Rank> prev(Rank r) {
        int i = index(r);
        if (i - 1 >= 0) return Optional.of(order.get(i - 1));
        return Optional.empty();
    }
}
