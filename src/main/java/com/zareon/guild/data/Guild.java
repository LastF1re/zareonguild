package com.zareon.guild.data;

import com.zareon.guild.region.CustomRegion;

import java.util.*;

public class Guild {
    private String name;
    private String tag;
    private int level;
    private final Map<UUID, GuildMember> members = new HashMap<>();
    private final List<CustomRegion> regions = new ArrayList<>();
    private final UUID leader;

    public Guild(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.level = 1;
        this.tag = "<aqua>" + name + "</aqua>";
        members.put(leader, new GuildMember(leader, Rank.LEADER));
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public Map<UUID, GuildMember> getMembers() { return members; }
    public List<CustomRegion> getRegions() { return regions; }

    public UUID getLeader() { return leader; }

    public Optional<GuildMember> getMember(UUID uuid) {
        return Optional.ofNullable(members.get(uuid));
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public void addMember(UUID uuid, Rank rank) {
        members.put(uuid, new GuildMember(uuid, rank));
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
