package com.zareon.guild.models;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildMember implements ConfigurationSerializable {
    private UUID playerId;
    private GuildRank rank;
    private long joinedTime;

    public GuildMember(UUID playerId, GuildRank rank) {
        this.playerId = playerId;
        this.rank = rank;
        this.joinedTime = System.currentTimeMillis();
    }

    public GuildMember(Map<String, Object> map) {
        this.playerId = UUID.fromString((String) map.get("playerId"));
        this.rank = GuildRank.valueOf((String) map.get("rank"));
        this.joinedTime = (long) map.get("joinedTime");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId.toString());
        map.put("rank", rank.name());
        map.put("joinedTime", joinedTime);
        return map;
    }

    public boolean promote() {
        switch (rank) {
            case BEGINNER:
                rank = GuildRank.VETERAN;
                return true;
            case VETERAN:
                rank = GuildRank.MASTER;
                return true;
            case MASTER:
                rank = GuildRank.RIGHTHAND;
                return true;
            default:
                return false; // Can't promote further
        }
    }

    public boolean demote() {
        switch (rank) {
            case RIGHTHAND:
                rank = GuildRank.MASTER;
                return true;
            case MASTER:
                rank = GuildRank.VETERAN;
                return true;
            case VETERAN:
                rank = GuildRank.BEGINNER;
                return true;
            default:
                return false; // Can't demote further
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return Bukkit.getOfflinePlayer(playerId).getName();
    }

    public GuildRank getRank() {
        return rank;
    }

    public void setRank(GuildRank rank) {
        this.rank = rank;
    }

    public long getJoinedTime() {
        return joinedTime;
    }
}
