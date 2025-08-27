package com.zareon.guild.utils;

import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import com.zareon.guild.models.GuildRank;
import org.bukkit.entity.Player;

public class PermissionUtils {

    /**
     * Проверяет, имеет ли игрок определенное разрешение в гильдии
     */
    public static boolean hasPermission(Player player, Guild guild, String permission) {
        if (guild == null) return false;

        GuildMember member = guild.getMember(player.getUniqueId());
        if (member == null) return false;

        // Лидер имеет все права
        if (guild.getLeaderId().equals(player.getUniqueId())) {
            return true;
        }

        GuildRank rank = member.getRank();
        if (rank == null) return false;

        return rank.hasPermission(permission);
    }

    /**
     * Проверяет, может ли игрок управлять другим участником
     */
    public static boolean canManageMember(Player manager, GuildMember target, Guild guild) {
        if (guild == null || target == null) return false;

        // Лидер может управлять всеми
        if (guild.getLeaderId().equals(manager.getUniqueId())) {
            return true;
        }

        GuildMember managerMember = guild.getMember(manager.getUniqueId());
        if (managerMember == null) return false;

        // Нельзя управлять участниками равного или выше ранга
        return managerMember.getRank().getPriority() > target.getRank().getPriority();
    }

    /**
     * Проверяет, может ли игрок приглашать новых участников
     */
    public static boolean canInvite(Player player, Guild guild) {
        return hasPermission(player, guild, "guild.invite");
    }

    /**
     * Проверяет, может ли игрок исключать участников
     */
    public static boolean canKick(Player player, Guild guild) {
        return hasPermission(player, guild, "guild.kick");
    }

    /**
     * Проверяет, может ли игрок строить в регионе гильдии
     */
    public static boolean canBuild(Player player, Guild guild) {
        return hasPermission(player, guild, "guild.build");
    }

    /**
     * Проверяет, может ли игрок управлять экономикой гильдииManageEconomy(Player player, Guild guild) {
     return hasPermission(player, guild, "guild.economy");
     }

     /**
     * Проверяет, может ли игрок управлять рангами
     */
    public static boolean canManageRanks(Player player, Guild guild) {
        return hasPermission(player, guild, "guild.ranks");
    }

    /**
     * Проверяет, может ли игрок использовать TNT
     */
    public static boolean canUseTNT(Player player, Guild guild) {
        return hasPermission(player, guild, "guild.tnt");
    }
}
