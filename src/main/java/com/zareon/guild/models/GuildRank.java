package com.zareon.guild.models;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum GuildRank {
    BEGINNER(new String[]{
            "USE_PRIVATE_BLOCKS",
            "USE_TNT"
    }),
    VETERAN(new String[]{
            "USE_PRIVATE_BLOCKS",
            "USE_TNT",
            "INVITE_MEMBERS"
    }),
    MASTER(new String[]{
            "USE_PRIVATE_BLOCKS",
            "USE_TNT",
            "INVITE_MEMBERS",
            "CRAFT_PRIVATE_BLOCKS",
            "CRAFT_TNT"
    }),
    RIGHTHAND(new String[]{
            "USE_PRIVATE_BLOCKS",
            "USE_TNT",
            "INVITE_MEMBERS",
            "CRAFT_PRIVATE_BLOCKS",
            "CRAFT_TNT",
            "PROMOTE_MEMBERS",
            "DEMOTE_MEMBERS"
    }),
    LEADER(new String[]{"*"});

    private final Set<String> permissions;

    GuildRank(String[] permissions) {
        this.permissions = new HashSet<>(Arrays.asList(permissions));
    }

    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getDisplayName() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }

    public int getLevel() {
        return ordinal();
    }
}
