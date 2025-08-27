package com.zareon.guild.listeners;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.utils.PermissionUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {
    private final ZareonGuild plugin;

    public BlockListener(ZareonGuild plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Проверяем, находится ли блок в регионе гильдии
        String regionOwner = plugin.getRegionManager().getRegionOwner(event.getBlock().getLocation());

        if (regionOwner != null) {
            Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

            // Если игрок не в гильдии
            if (playerGuild == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете строить в регионе гильдии " + regionOwner + "!");
                return;
            }

            // Если игрок не в той же гильдии, что и владелец региона
            if (!playerGuild.getName().equals(regionOwner)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете строить в регионе гильдии " + regionOwner + "!");
                return;
            }

            // Проверяем права на строительство в регионе своей гильдии
            if (!PermissionUtils.canBuildInRegion(player, playerGuild)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "У вас нет прав на строительство в регионах гильдии!");
                return;
            }
        }

        // Специальная обработка для TNT
        if (event.getBlock().getType() == Material.TNT) {
            Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

            if (playerGuild == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Только участники гильдий могут размещать TNT!");
                return;
            }

            if (!PermissionUtils.canUseTNT(player, playerGuild)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "У вас нет прав на использование TNT!");
                return;
            }

            if (!plugin.getTntManager().canUseTNT(player)) {
                long remaining = plugin.getTntManager().getRemainingCooldown(player);
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "TNT находится на перезарядке! Осталось: " +
                        plugin.getTntManager().formatCooldown(remaining));
                return;
            }

            // Пытаемся разместить TNT из хранилища гильдии
            if (!plugin.getTntManager().placeTNT(player, event.getBlock().getLocation(), playerGuild.getName())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "В хранилище гильдии недостаточно TNT!");
                return;
            }

            player.sendMessage(ChatColor.GREEN + "TNT размещен! В хранилище осталось: " +
                    plugin.getTntManager().getGuildTNT(playerGuild.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Проверяем, находится ли блок в регионе гильдии
        String regionOwner = plugin.getRegionManager().getRegionOwner(event.getBlock().getLocation());

        if (regionOwner != null) {
            Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

            // Если игрок не в гильдии
            if (playerGuild == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете ломать блоки в регионе гильдии " + regionOwner + "!");
                return;
            }

            // Если игрок не в той же гильдии, что и владелец региона
            if (!playerGuild.getName().equals(regionOwner)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете ломать блоки в регионе гильдии " + regionOwner + "!");
                return;
            }

            // Проверяем права на строительство (включает в себя и разрушение) в регионе своей гильдии
            if (!PermissionUtils.canBuildInRegion(player, playerGuild)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "У вас нет прав на изменение блоков в регионах гильдии!");
                return;
            }
        }

        // Специальная обработка для TNT
        if (event.getBlock().getType() == Material.TNT) {
            Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

            if (playerGuild != null && PermissionUtils.canUseTNT(player, playerGuild)) {
                // Возвращаем TNT в хранилище гильдии при его ломании
                if (plugin.getTntManager().addTNTToGuild(playerGuild.getName(), 1)) {
                    player.sendMessage(ChatColor.GREEN + "TNT возвращен в хранилище гильдии!");
                }
            }
        }
    }
}
