package com.zareon.guild.data;

public enum Rank {
    BEGINNER,
    VETERAN,
    MASTER,
    RIGHT_HAND,
    LEADER;

    public static Rank fromString(String s) {
        try {
            return Rank.valueOf(s.toUpperCase().replace('-', '_'));
        } catch (Exception e) {
            return BEGINNER;
        }
    }
}
