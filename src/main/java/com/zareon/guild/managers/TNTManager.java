package com.zareon.guild.managers;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.utils.PermissionUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TNTManager {

    private final ZareonGuild plugin;
    private final Map<UUID, TNTSettings> guildTNTSettings = new HashMap<>();
    private final Set<UUID> tntModeEnabled = new HashSet<>();

    public TNTManager(ZareonGuild plugin) {
        this.plugin = plugin;
        loadTNTSettings();
    }

    /**
     * Настройки TNT для гильдии
     */
    public static class TNTSettings {
        private boolean tntEnabled;
        private int maxTNTPerPlayer;
        private long tntCooldown; // в миллисекундах
        private boolean allowTNTInOwnRegion;
        private Set<Material> protectedBlocks;

        public TNTSettings() {
            this.tntEnabled = true;
            this.maxTNTPerPlayer = 64;
            this.tntCooldown = 5000; // 5 секунд
            this.allowTNTInOwnRegion = false;
            this.protectedBlocks = new HashSet<>(Arrays.asList(
                    Material.BEDROCK, Material.OBSIDIAN, Material.ENCHANTING_TABLE
            ));
        }

        // Геттеры и сеттеры
        public boolean isTntEnabled() { return tntEnabled; }
        public void setTntEnabled(boolean tntEnabled) { this.tntEnabled = tntEnabled; }

        public int getMaxTNTPerPlayer() { return maxTNTPerPlayer; }
        public void setMaxTNTPerPlayer(int maxTNTPerPlayer) { this.maxTNTPerPlayer = maxTNTPerPlayer; }

        public long getTntCooldown() { return tntCooldown; }
        public void setTntCooldown(long tntCooldown) { this.tntCooldown = tntCooldown; }

        public boolean isAllowTNTInOwnRegion() { return allowTNTInOwnRegion; }
        public void setAllowTNTInOwnRegion(boolean allowTNTInOwnRegion) { this.allowTNTInOwnRegion = allowTNTInOwnRegion; }

        public Set<Material> getProtectedBlocks() { return protectedBlocks; }
        public void setProtectedBlocks(Set<Material> protectedBlocks) { this.protectedBlocks = protectedBlocks; }
    }

    private final Map<UUID, Long> lastTNTUsage = new HashMap<>();
    private final Map<UUID, Integer> playerTNTCount = new HashMap<>();

    /**
     * Загружает настройки TNT для всех гильдий
     */
    private void loadTNTSettings() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "SELECT * FROM guild_tnt_settings";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID guildId = UUID.fromString(rs.getString("guild_id"));
                    TNTSettings settings = new TNTSettings();

                    settings.setTntEnabled(rs.getBoolean("tnt_enabled"));
                    settings.setMaxTNTPerPlayer(rs.getInt("max_tnt_per_player"));
                    settings.setTntCooldown(rs.getLong("tnt_cooldown"));
                    settings.setAllowTNTInOwnRegion(rs.getBoolean("allow_in_own_region"));

                    String protectedBlocksStr = rs.getString("protected_blocks");
                    if (protectedBlocksStr != null && !protectedBlocksStr.isEmpty()) {
                        Set<Material> protectedBlocks = new HashSet<>();
                        for (String materialName : protectedBlocksStr.split(",")) {
                            try {
                                protectedBlocks.add(Material.valueOf(materialName));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        settings.setProtectedBlocks(protectedBlocks);
                    }

                    guildTNTSettings.put(guildId, settings);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка загрузки настроек TNT: " + e.getMessage());
        }
    }

    /**
     * Получает настройки TNT для гильдии
     */
    public TNTSettings getTNTSettings(UUID guildId) {
        return guildTNTSettings.computeIfAbsent(guildId, k -> new TNTSettings());
    }

    /**
     * Обновляет настройки TNT для гильдии
     */
    public void updateTNTSettings(UUID guildId, TNTSettings settings) {
        guildTNTSettings.put(guildId, settings);
        saveTNTSettings(guildId, settings);
    }

    /**
     * Проверяет, может ли игрок использовать TNT
     */
    public boolean canUseTNT(Player player, Location location) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return plugin.getConfig().getBoolean("tnt.allow-without-guild", false);
        }

        // Проверяем права в гильдии
        if (!PermissionUtils.canUseTNT(player, guild)) {
            return false;
        }

        TNTSettings settings = getTNTSettings(guild.getId());

        // Проверяем, включен ли TNT для гильдии
        if (!settings.isTntEnabled()) {
            return false;
        }

        // Проверяем кулдаун
        Long lastUsage = lastTNTUsage.get(player.getUniqueId());
        if (lastUsage != null && System.currentTimeMillis() - lastUsage < settings.getTntCooldown()) {
            return false;
        }

        // Проверяем лимит TNT
        int currentCount = playerTNTCount.getOrDefault(player.getUniqueId(), 0);
        if (currentCount >= settings.getMaxTNTPerPlayer()) {
            return false;
        }

        // Проверяем, находится ли в собственном регионе
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            UUID regionGuildId = getRegionGuildId(location);
            if (regionGuildId != null && regionGuildId.equals(guild.getId()) &&
                    !settings.isAllowTNTInOwnRegion()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Выдает TNT игроку
     */
    public boolean giveTNT(Player player, int amount) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return false;

        if (!PermissionUtils.canUseTNT(player, guild)) {
            return false;
        }

        TNTSettings settings = getTNTSettings(guild.getId());
        int currentCount = playerTNTCount.getOrDefault(player.getUniqueId(), 0);

        if (currentCount + amount > settings.getMaxTNTPerPlayer()) {
            amount = settings.getMaxTNTPerPlayer() - currentCount;
        }

        if (amount <= 0) return false;

        ItemStack tnt = new ItemStack(Material.TNT, amount);
        player.getInventory().addItem(tnt);

        playerTNTCount.put(player.getUniqueId(), currentCount + amount);

        return true;
    }

    /**
     * Обрабатывает установку TNT
     */
    public boolean handleTNTPlace(Player player, Location location) {
        if (!canUseTNT(player, location)) {
            return false;
        }

        lastTNTUsage.put(player.getUniqueId(), System.currentTimeMillis());

        // Уменьшаем счетчик TNT у игрока
        int currentCount = playerTNTCount.getOrDefault(player.getUniqueId(), 0);
        if (currentCount > 0) {
            playerTNTCount.put(player.getUniqueId(), currentCount - 1);
        }

        return true;
    }

    /**
     * Обрабатывает взрыв TNT
     */
    public void handleTNTExplosion(TNTPrimed tnt, List<Location> blocksToDestroy) {
        Location explosionLocation = tnt.getLocation();

        // Получаем гильдию региона взрыва
        UUID regionGuildId = getRegionGuildId(explosionLocation);
        if (regionGuildId == null) return;

        TNTSettings settings = getTNTSettings(regionGuildId);

        // Удаляем защищенные блоки из списка разрушения
        blocksToDestroy.removeIf(location -> {
            Material blockType = location.getBlock().getType();
            return settings.getProtectedBlocks().contains(blockType);
        });
    }

    /**
     * Включает/выключает режим TNT для игрока
     */
    public void toggleTNTMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (tntModeEnabled.contains(playerId)) {
            tntModeEnabled.remove(playerId);
        } else {
            tntModeEnabled.add(playerId);
        }
    }

    /**
     * Проверяет, включен ли режим TNT у игрока
     */
    public boolean isTNTModeEnabled(Player player) {
        return tntModeEnabled.contains(player.getUniqueId());
    }

    /**
     * Получает оставшееся время кулдауна
     */
    public long getRemainingCooldown(Player player) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return 0;

        TNTSettings settings = getTNTSettings(guild.getId());
        Long lastUsage = lastTNTUsage.get(player.getUniqueId());

        if (lastUsage == null) return 0;

        long elapsed = System.currentTimeMillis() - lastUsage;
        return Math.max(0, settings.getTntCooldown() - elapsed);
    }

    /**
     * Получает текущий счетчик TNT игрока
     */
    public int getPlayerTNTCount(Player player) {
        return playerTNTCount.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Сбрасывает счетчик TNT игрока
     */
    public void resetPlayerTNTCount(Player player) {
        playerTNTCount.remove(player.getUniqueId());
    }

    /**
     * Получает ID гильдии по региону в локации
     */
    private UUID getRegionGuildId(Location location) {
        if (!plugin.getRegionManager().isWorldGuardEnabled()) return null;

        com.sk89q.worldguard.protection.regions.ProtectedRegion region =
                plugin.getRegionManager().getRegionAt(location);

        if (region == null) return null;

        return plugin.getRegionManager().getGuildByRegion(region.getId());
    }

    /**
     * Сохраняет настройки TNT в базу данных
     */
    private void saveTNTSettings(UUID guildId, TNTSettings settings) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "INSERT INTO guild_tnt_settings " +
                    "(guild_id, tnt_enabled, max_tnt_per_player, tnt_cooldown, " +
                    "allow_in_own_region, protected_blocks) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "tnt_enabled = VALUES(tnt_enabled), " +
                    "max_tnt_per_player = VALUES(max_tnt_per_player), " +
                    "tnt_cooldown = VALUES(tnt_cooldown), " +
                    "allow_in_own_region = VALUES(allow_in_own_region), " +
                    "protected_blocks = VALUES(protected_blocks)";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, guildId.toString());
                stmt.setBoolean(2, settings.isTntEnabled());
                stmt.setInt(3, settings.getMaxTNTPerPlayer());
                stmt.setLong(4, settings.getTntCooldown());
                stmt.setBoolean(5, settings.isAllowTNTInOwnRegion());

                StringBuilder protectedBlocks = new StringBuilder();
                for (Material material : settings.getProtectedBlocks()) {
                    if (protectedBlocks.length() > 0) protectedBlocks.append(",");
                    protectedBlocks.append(material.name());
                }
                stmt.setString(6, protectedBlocks.toString());

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка сохранения настроек TNT: " + e.getMessage());
        }
    }

    /**
     * Удаляет настройки TNT гильдии
     */
    public void deleteTNTSettings(UUID guildId) {
        guildTNTSettings.remove(guildId);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "DELETE FROM guild_tnt_settings WHERE guild_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, guildId.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка удаления настроек TNT: " + e.getMessage());
        }
    }

    /**
     * Очищает данные игрока при выходе
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        tntModeEnabled.remove(playerId);
        lastTNTUsage.remove(playerId);
        playerTNTCount.remove(playerId);
    }
}
