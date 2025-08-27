package com.zareon.guild.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Guild implements ConfigurationSerializable {
    private UUID id;
    private String name;
    private String tag;
    private int level;
    private Map<UUID, GuildMember> members;
    private UUID leader;
    private List<GuildRegion> regions;
    private Map<String, Boolean> flags;
    private long creationTime;

    public Guild(String name, Player leader) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.tag = name.substring(0, Math.min(name.length(), 4)).toUpperCase();
        this.level = 1;
        this.members = new HashMap<>();
        this.leader = leader.getUniqueId();
        this.regions = new ArrayList<>();
        this.flags = new HashMap<>();
        this.creationTime = Instant.now().getEpochSecond();

        // Add leader as member with Leader rank
        GuildMember leaderMember = new GuildMember(leader.getUniqueId(), GuildRank.LEADER);
        this.members.put(leader.getUniqueId(), leaderMember);
    }

    public Guild(Map<String, Object> map) {
        this.id = UUID.fromString((String) map.get("id"));
        this.name = (String) map.get("name");
        this.tag = (String) map.get("tag");
        this.level = (int) map.get("level");
        this.leader = UUID.fromString((String) map.get("leader"));
        this.creationTime = (long) map.get("creationTime");

        // Deserialize members
        this.members = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> membersList = (List<Map<String, Object>>) map.get("members");
        for (Map<String, Object> memberMap : membersList) {
            GuildMember member = new GuildMember(memberMap);
            this.members.put(member.getPlayerId(), member);
        }

        // Deserialize regions
        this.regions = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> regionsList = (List<Map<String, Object>>) map.get("regions");
        for (Map<String, Object> regionMap : regionsList) {
            GuildRegion region = new GuildRegion(regionMap);
            this.regions.add(region);
        }

        // Deserialize flags
        @SuppressWarnings("unchecked")
        this.flags = (Map<String, Boolean>) map.get("flags");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("name", name);
        map.put("tag", tag);
        map.put("level", level);
        map.put("leader", leader.toString());
        map.put("creationTime", creationTime);

        // Serialize members
        List<Map<String, Object>> membersList = members.values().stream()
                .map(GuildMember::serialize)
                .collect(Collectors.toList());
        map.put("members", membersList);

        // Serialize regions
        List<Map<String, Object>> regionsList = regions.stream()
                .map(GuildRegion::serialize)
                .collect(Collectors.toList());
        map.put("regions", regionsList);

        // Serialize flags
        map.put("flags", flags);

        return map;
    }

    // Guild operations
    public boolean addMember(Player player) {
        if (members.containsKey(player.getUniqueId())) {
            return false;
        }

        GuildMember member = new GuildMember(player.getUniqueId(), GuildRank.BEGINNER);
        members.put(player.getUniqueId(), member);
        return true;
    }

    public boolean removeMember(UUID playerId) {
        if (!members.containsKey(playerId)) {
            return false;
        }

        if (playerId.equals(leader)) {
            return false; // Can't remove the leader
        }

        members.remove(playerId);
        return true;
    }

    public boolean promoteMember(UUID playerId) {
        GuildMember member = members.get(playerId);
        if (member == null) {
            return false;
        }

        return member.promote();
    }

    public boolean demoteMember(UUID playerId) {
        GuildMember member = members.get(playerId);
        if (member == null) {
            return false;
        }

        return member.demote();
    }

    public boolean setLeader(UUID playerId) {
        if (!members.containsKey(playerId)) {
            return false;
        }

        // Update old leader
        GuildMember oldLeader = members.get(leader);
        oldLeader.setRank(GuildRank.RIGHTHAND);

        // Update new leader
        GuildMember newLeader = members.get(playerId);
        newLeader.setRank(GuildRank.LEADER);

        leader = playerId;
        return true;
    }

    public boolean addRegion(Location center, int radius, int height, String type) {
        GuildRegion region = new GuildRegion(center, radius, height, type);
        regions.add(region);
        return true;
    }

    public boolean removeRegion(UUID regionId) {
        return regions.removeIf(region -> region.getId().equals(regionId));
    }

    public boolean levelUp() {
        if (level >= 5) {
            return false;
        }

        level++;
        return true;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<UUID, GuildMember> getMembers() {
        return members;
    }

    public UUID getLeader() {
        return leader;
    }

    public String getLeaderName() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(leader);
        return player.getName();
    }

    public List<GuildRegion> getRegions() {
        return regions;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    public boolean getFlag(String flag) {
        return flags.getOrDefault(flag, false);
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public GuildMember getMember(UUID playerId) {
        return members.get(playerId);
    }

    public boolean hasPermission(UUID playerId, String permission) {
        GuildMember member = members.get(playerId);
        if (member == null) {
            return false;
        }

        return member.getRank().hasPermission(permission);
    }
}
