package com.zareon.guild.manager;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.data.GuildMember;
import com.zareon.guild.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildManager {
    private final ZareonGuildPlugin plugin;
    private final RankManager ranks;

    private final Map<String, Guild> byName = new HashMap<>();
    private final Map<UUID, String> byPlayer = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>(); // player -> guild name

    private final File store;

    public GuildManager(ZareonGuildPlugin plugin, RankManager ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.store = new File(plugin.getDataFolder(), "guilds.yml");
        loadAll();
    }

    public synchronized void loadAll() {
        byName.clear();
        byPlayer.clear();
        if (!store.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(store);
        for (String gName : yml.getKeys(false)) {
            String path = gName + ".";
            UUID leader = UUID.fromString(yml.getString(path + "leader"));
            Guild g = new Guild(gName, leader);
            g.setTag(yml.getString(path + "tag", "<aqua>" + gName + "</aqua>"));
            g.setLevel(yml.getInt(path + "level", 1));
            // members
            for (String uuidStr : yml.getConfigurationSection(path + "members").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Rank r = Rank.fromString(yml.getString(path + "members." + uuidStr));
                g.addMember(uuid, r);
                byPlayer.put(uuid, gName);
            }
            byName.put(gName.toLowerCase(), g);
        }
    }

    public synchronized void saveAll() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Guild g : byName.values()) {
            String base = g.getName() + ".";
            yml.set(base + "leader", g.getLeader().toString());
            yml.set(base + "tag", g.getTag());
            yml.set(base + "level", g.getLevel());
            Map<String, String> members = new HashMap<>();
            for (GuildMember m : g.getMembers().values()) {
                members.put(m.getUuid().toString(), m.getRank().name());
            }
            yml.createSection(base + "members", members);
        }
        try {
            yml.save(store);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save guilds.yml: " + e.getMessage());
        }
    }

    public synchronized boolean nameTaken(String name) {
        return byName.containsKey(name.toLowerCase());
    }

    public synchronized Guild create(String name, UUID leader) {
        Guild g = new Guild(name, leader);
        byName.put(name.toLowerCase(), g);
        byPlayer.put(leader, name.toLowerCase());
        saveAll();
        return g;
    }

    public synchronized void disband(Guild g) {
        byName.remove(g.getName().toLowerCase());
        for (UUID u : new ArrayList<>(g.getMembers().keySet())) {
            byPlayer.remove(u);
        }
        saveAll();
    }

    public Guild getByName(String name) {
        return byName.get(name.toLowerCase());
    }

    public Guild getGuildByPlayer(UUID uuid) {
        String name = byPlayer.get(uuid);
        return name != null ? byName.get(name) : null;
    }

    public Rank getRank(UUID uuid) {
        Guild g = getGuildByPlayer(uuid);
        if (g == null) return null;
        return g.getMember(uuid).map(GuildMember::getRank).orElse(null);
    }

    public void addInvite(UUID player, String guildName) {
        invites.put(player, guildName.toLowerCase());
        Bukkit.getScheduler().runTaskLater(plugin, () -> invites.remove(player), 20L * 60 * 5); // 5 min expiry
    }

    public Optional<Guild> consumeInvite(UUID player) {
        String name = invites.remove(player);
        return name != null ? Optional.ofNullable(byName.get(name)) : Optional.empty();
    }

    public boolean join(Guild g, UUID player) {
        if (getGuildByPlayer(player) != null) return false;
        g.addMember(player, Rank.BEGINNER);
        byPlayer.put(player, g.getName().toLowerCase());
        saveAll();
        return true;
    }

    public boolean leave(UUID player) {
        Guild g = getGuildByPlayer(player);
        if (g == null) return false;
        if (g.isLeader(player)) return false;
        g.removeMember(player);
        byPlayer.remove(player);
        saveAll();
        return true;
    }

    public boolean rename(Guild g, String newName) {
        if (nameTaken(newName)) return false;
        byName.remove(g.getName().toLowerCase());
        g.setName(newName);
        byName.put(newName.toLowerCase(), g);
        // update byPlayer mapping keys are UUID, values are name; update all members to new value
        for (UUID u : g.getMembers().keySet()) {
            byPlayer.put(u, newName.toLowerCase());
        }
        saveAll();
        return true;
    }

    public boolean promote(Guild g, UUID actor, UUID target) {
        Rank a = g.getMember(actor).map(GuildMember::getRank).orElse(null);
        Rank t = g.getMember(target).map(GuildMember::getRank).orElse(null);
        if (a == null || t == null) return false;
        if (!plugin.ranks().canPromote(a, t)) return false;
        var next = plugin.ranks().next(t);
        if (next.isEmpty()) return false;
        g.getMember(target).ifPresent(m -> m.setRank(next.get()));
        saveAll();
        return true;
    }

    public boolean demote(Guild g, UUID actor, UUID target) {
        Rank a = g.getMember(actor).map(GuildMember::getRank).orElse(null);
        Rank t = g.getMember(target).map(GuildMember::getRank).orElse(null);
        if (a == null || t == null) return false;
        if (!plugin.ranks().canDemote(a, t)) return false;
        var prev = plugin.ranks().prev(t);
        if (prev.isEmpty()) return false;
        g.getMember(target).ifPresent(m -> m.setRank(prev.get()));
        saveAll();
        return true;
    }

    public boolean isMember(Guild g, UUID uuid) {
        return g != null && g.getMembers().containsKey(uuid);
    }

    public boolean canUseLevel(Guild g, int required) {
        return g != null && g.getLevel() >= required;
    }

    public boolean setLevel(Guild g, int newLevel) {
        g.setLevel(newLevel);
        saveAll();
        return true;
    }

    public Map<UUID, GuildMember> members(Guild g) {
        return Collections.unmodifiableMap(g.getMembers());
    }

    public OfflinePlayer offline(UUID id) {
        return Bukkit.getOfflinePlayer(id);
    }
}
