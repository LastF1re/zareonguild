package com.zareon.guild.managers;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {
    private final ZareonGuild plugin;
    private final Map<UUID, Guild> guilds;
    private final Map<UUID, UUID> playerGuildMap; // Maps player UUID to guild UUID
    private final Map<String, UUID> guildNameMap; // Maps guild name to guild UUID
    private final Map<UUID, UUID> invitationMap; // Maps player UUID to guild UUID (invitation)
    private final File guildsFile;

    public GuildManager(ZareonGuild plugin) {
        this.plugin = plugin;
        this.guilds = new ConcurrentHashMap<>();
        this.playerGuildMap = new ConcurrentHashMap<>();
        this.guildNameMap = new ConcurrentHashMap<>();
        this.invitationMap = new ConcurrentHashMap<>();
        this.guildsFile = new File(plugin.getDataFolder(), "guilds.yml");
    }

    public void loadGuilds() {
        if (!guildsFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(guildsFile);

        if (!config.isConfigurationSection("guilds")) {
            return;
        }

        for (String key : config.getConfigurationSection("guilds").getKeys(false)) {
            UUID guildId = UUID.fromString(key);
            Map<String, Object> guildData = config.getConfigurationSection("guilds." + key).getValues(true);
            Guild guild = new Guild(guildData);

            guilds.put(guildId, guild);
            guildNameMap.put(guild.getName().toLowerCase(), guildId);

            // Update player guild map
            for (UUID playerId : guild.getMembers().keySet()) {
                playerGuildMap.put(playerId, guildId);
            }
        }

        plugin.getLogger().info("Loaded " + guilds.size() + " guilds.");
    }

    public void saveGuilds() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Guild> entry : guilds.entrySet()) {
            UUID guildId = entry.getKey();
            Guild guild = entry.getValue();

            config.set("guilds." + guildId.toString(), guild.serialize());
        }

        try {
            config.save(guildsFile);
            plugin.getLogger().info("Saved " + guilds.size() + " guilds.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save guilds: " + e.getMessage());
        }
    }

    public Guild createGuild(String name, Player leader) {
        // Check if name is already taken
        if (getGuildByName(name) != null) {
            return null;
        }

        // Check if player is already in a guild
        if (getGuildByPlayer(leader.getUniqueId()) != null) {
            return null;
        }

        // Check if player has enough money
        double creationCost = plugin.getConfigManager().getCreationCost();
        if (!plugin.getEconomyManager().hasEnough(leader, creationCost)) {
            return null;
        }

        // Charge the player
        plugin.getEconomyManager().withdraw(leader, creationCost);

        // Create the guild
        Guild guild = new Guild(name, leader);

        // Add to maps
        guilds.put(guild.getId(), guild);
        playerGuildMap.put(leader.getUniqueId(), guild.getId());
        guildNameMap.put(name.toLowerCase(), guild.getId());

        // Save if configured to do so
        if (plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return guild;
    }

    public boolean disbandGuild(UUID guildId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        // Remove all members from player guild map
        for (UUID playerId : guild.getMembers().keySet()) {
            playerGuildMap.remove(playerId);
        }

        // Remove from maps
        guilds.remove(guildId);
        guildNameMap.remove(guild.getName().toLowerCase());

        // Save if configured to do so
        if (plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return true;
    }

    public boolean invitePlayer(UUID guildId, UUID inviterId, UUID targetId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        // Check if inviter has permission
        if (!guild.hasPermission(inviterId, "INVITE_MEMBERS")) {
            return false;
        }

        // Check if target is already in a guild
        if (getGuildByPlayer(targetId) != null) {
            return false;
        }

        // Add invitation
        invitationMap.put(targetId, guildId);

        // Schedule invitation expiration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (invitationMap.containsKey(targetId) && invitationMap.get(targetId).equals(guildId)) {
                invitationMap.remove(targetId);

                // Notify player if online
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    target.sendMessage(plugin.getConfigManager().getMessage("invitation.expired"));
                }
            }
        }, 20 * 60); // 1 minute expiration

        return true;
    }

    public boolean acceptInvitation(UUID playerId) {
        if (!invitationMap.containsKey(playerId)) {
            return false;
        }

        UUID guildId = invitationMap.get(playerId);
        Guild guild = guilds.get(guildId);

        if (guild == null) {
            invitationMap.remove(playerId);
            return false;
        }

        // Add player to guild
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            invitationMap.remove(playerId);
            return false;
        }

        boolean success = guild.addMember(player);
        if (success) {
            playerGuildMap.put(playerId, guildId);
            invitationMap.remove(playerId);

            // Save if configured to do so
            if (plugin.getConfigManager().isSaveOnChange()) {
                saveGuilds();
            }
        }

        return success;
    }

    public boolean leaveGuild(UUID playerId) {
        Guild guild = getGuildByPlayer(playerId);
        if (guild == null) {
            return false;
        }

        // Check if player is the leader
        if (playerId.equals(guild.getLeader())) {
            return false; // Leader can't leave, they need to disband or transfer leadership
        }

        boolean success = guild.removeMember(playerId);
        if (success) {
            playerGuildMap.remove(playerId);

            // Save if configured to do so
            if (plugin.getConfigManager().isSaveOnChange()) {
                saveGuilds();
            }
        }

        return success;
    }

    public boolean renameGuild(UUID guildId, String newName) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        // Check if name is already taken
        if (getGuildByName(newName) != null) {
            return false;
        }

        String oldName = guild.getName();
        guild.setName(newName);

        // Update name map
        guildNameMap.remove(oldName.toLowerCase());
        guildNameMap.put(newName.toLowerCase(), guildId);

        // Save if configured to do so
        if (plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return true;
    }

    public boolean promoteMember(UUID promoterId, UUID targetId) {
        Guild guild = getGuildByPlayer(promoterId);
        if (guild == null) {
            return false;
        }

        // Check if promoter has permission
        if (!guild.hasPermission(promoterId, "PROMOTE_MEMBERS")) {
            return false;
        }

        // Check if target is in the guild
        if (!guild.isMember(targetId)) {
            return false;
        }

        boolean success = guild.promoteMember(targetId);

        // Save if configured to do so
        if (success && plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return success;
    }

    public boolean demoteMember(UUID demoterId, UUID targetId) {
        Guild guild = getGuildByPlayer(demoterId);
        if (guild == null) {
            return false;
        }

        // Check if demoter has permission
        if (!guild.hasPermission(demoterId, "DEMOTE_MEMBERS")) {
            return false;
        }

        // Check if target is in the guild
        if (!guild.isMember(targetId)) {
            return false;
        }

        boolean success = guild.demoteMember(targetId);

        // Save if configured to do so
        if (success && plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return success;
    }

    public boolean levelUpGuild(UUID guildId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        boolean success = guild.levelUp();

        // Save if configured to do so
        if (success && plugin.getConfigManager().isSaveOnChange()) {
            saveGuilds();
        }

        return success;
    }

    // Getters
    public Guild getGuildById(UUID guildId) {
        return guilds.get(guildId);
    }

    public Guild getGuildByName(String name) {
        UUID guildId = guildNameMap.get(name.toLowerCase());
        return guildId != null ? guilds.get(guildId) : null;
    }

    public Guild getGuildByPlayer(UUID playerId) {
        UUID guildId = playerGuildMap.get(playerId);
        return guildId != null ? guilds.get(guildId) : null;
    }

    public boolean hasInvitation(UUID playerId) {
        return invitationMap.containsKey(playerId);
    }

    public Guild getInvitationGuild(UUID playerId) {
        UUID guildId = invitationMap.get(playerId);
        return guildId != null ? guilds.get(guildId) : null;
    }

    public Collection<Guild> getAllGuilds() {
        return guilds.values();
    }
}
