package com.zareon.guild.listeners;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildRegion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final ZareonGuild plugin;
    private final Map<UUID, UUID> playerCurrentRegion; // UUID игрока -> UUID региона
    private final boolean showRegionMessages;
    private final boolean showWelcomeMessage;

    public PlayerListener(ZareonGuild plugin) {
        this.plugin = plugin;
        this.playerCurrentRegion = new HashMap<>();
        this.showRegionMessages = plugin.getConfig().getBoolean("regions.show-enter-exit-messages", true);
        this.showWelcomeMessage = plugin.getConfig().getBoolean("guild.show-welcome-message", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в гильдии
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerId);

        if (guild != null && showWelcomeMessage) {
            // Отправляем приветственное сообщение участнику гильдии
            player.sendMessage(ChatColor.GREEN + "Добро пожаловать, " + player.getName() + "!");
            player.sendMessage(ChatColor.YELLOW + "Вы состоите в гильдии: " + ChatColor.GOLD + guild.getName());

            // Показываем информацию о гильдии
            int membersOnline = 0;
            for (UUID memberId : guild.getMembers().keySet()) {
                if (plugin.getServer().getPlayer(memberId) != null) {
                    membersOnline++;
                }
            }

            player.sendMessage(ChatColor.GRAY + "Участников онлайн: " + ChatColor.WHITE + membersOnline +
                    ChatColor.GRAY + "/" + ChatColor.WHITE + guild.getMembers().size());

            // Показываем баланс гильдии
            if (plugin.getEconomyManager().hasEconomy()) {
                double balance = plugin.getEconomyManager().getGuildBalance(guild.getName());
                player.sendMessage(ChatColor.GRAY + "Баланс гильдии: " + ChatColor.GREEN +
                        plugin.getEconomyManager().formatMoney(balance));
            }
        }

        // Инициализируем текущий регион игрока
        updatePlayerRegion(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Очищаем данные об игроке
        playerCurrentRegion.remove(playerId);

        // Уведомляем участников гильдии о выходе
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerId);
        if (guild != null) {
            String message = ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " покинул игру";

            for (UUID memberId : guild.getMembers().keySet()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null && !member.equals(player)) {
                    member.sendMessage(message);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Проверяем изменение региона только при смене блока
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            updatePlayerRegion(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Обновляем регион после телепортации
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updatePlayerRegion(player);
        }, 1L);
    }

    private void updatePlayerRegion(Player player) {
        if (!showRegionMessages) return;

        UUID playerId = player.getUniqueId();
        GuildRegion currentRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
        UUID currentRegionId = currentRegion != null ? currentRegion.getId() : null;
        UUID previousRegionId = playerCurrentRegion.get(playerId);

        // Если регион изменился
        if (!java.util.Objects.equals(currentRegionId, previousRegionId)) {
            // Сообщение о выходе из предыдущего региона
            if (previousRegionId != null) {
                GuildRegion previousRegion = plugin.getRegionManager().getRegion(previousRegionId);
                if (previousRegion != null) {
                    player.sendMessage(ChatColor.RED + "◀ Вы покинули регион типа: " +
                            ChatColor.DARK_RED + previousRegion.getType());
                }
            }

            // Сообщение о входе в новый регион
            if (currentRegion != null) {
                String regionOwner = plugin.getRegionManager().getRegionOwner(player.getLocation());
                if (regionOwner != null) {
                    Guild playerGuild = plugin.getGuildManager().getPlayerGuild(playerId);

                    // Определяем тип сообщения в зависимости от принадлежности к гильдии
                    if (playerGuild != null && playerGuild.getName().equals(regionOwner)) {
                        // Свой регион
                        player.sendMessage(ChatColor.GREEN + "▶ Вы вошли в регион своей гильдии");
                        player.sendMessage(ChatColor.DARK_GREEN + "  Тип: " + currentRegion.getType() +
                                " | Радиус: " + currentRegion.getRadius() + "м");
                    } else {
                        // Чужой регион
                        player.sendMessage(ChatColor.YELLOW + "▶ Вы вошли в регион гильдии " +
                                ChatColor.GOLD + regionOwner);
                        player.sendMessage(ChatColor.GOLD + "  Тип: " + currentRegion.getType() +
                                " | Радиус: " + currentRegion.getRadius() + "м");
                        player.sendMessage(ChatColor.RED + "⚠ Здесь действуют ограничения!");
                    }
                }
            }

            // Обновляем текущий регион игрока
            if (currentRegionId != null) {
                playerCurrentRegion.put(playerId, currentRegionId);
            } else {
                playerCurrentRegion.remove(playerId);
            }
        }
    }

    // Вспомогательные методы для других систем
    public GuildRegion getPlayerCurrentRegion(Player player) {
        UUID regionId = playerCurrentRegion.get(player.getUniqueId());
        return regionId != null ? plugin.getRegionManager().getRegion(regionId) : null;
    }

    public void clearPlayerRegionData(Player player) {
        playerCurrentRegion.remove(player.getUniqueId());
    }

    public void notifyGuildMembers(Guild guild, String message) {
        for (UUID memberId : guild.getMembers().keySet()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }

    public void notifyGuildMembersExcept(Guild guild, String message, Player excludePlayer) {
        for (UUID memberId : guild.getMembers().keySet()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && !member.equals(excludePlayer)) {
                member.sendMessage(message);
            }
        }
    }
}
