package com.zareon.guild.data;

import java.util.UUID;

public class GuildMember {
    private final UUID uuid;
    private Rank rank;

    public GuildMember(UUID uuid, Rank rank) {
        this.uuid = uuid;
        this.rank = rank;
    }

    public UUID getUuid() { return uuid; }
    public Rank getRank() { return rank; }
    public void setRank(Rank rank) { this.rank = rank; }
}
