package com.zareon.guild.managers;

import com.zareon.guild.Zzareon.guild.models.Guild;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final ZareonGuild plugin;
    private Economy economy = null;
    private final Map<UUID, Double> guildBalances = new HashMap<>();

    public EconomyManager(ZareonGuild plugin) {
        this.plugin = plugin;
        setupEconomy();
        loadGuildBalances();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден! Экономическая система отключена.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Экономический плагин не найден!");
            return;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Подключен к экономической системе: " + economy.getName());
    }

    /**
     * Загружает балансы всех гильдий из базы данных
     */
    private void loadGuildBalances() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "SELECT guild_id, balance FROM guild_economy";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID guildId = UUID.fromString(rs.getString("guild_id"));
                    double balance = rs.getDouble("balance");
                    guildBalances.put(guildId, balance);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка загрузки балансов гильдий: " + e.getMessage());
        }
    }

    /**
     * Получает баланс гильдии
     */
    public double getGuildBalance(UUID guildId) {
        return guildBalances.getOrDefault(guildId, 0.0);
    }

    /**
     * Устанавливает баланс гильдии
     */
    public void setGuildBalance(UUID) {
        guildBalances.put(guildId, Math.max(0, amount));
        saveGuildBalance(guildId);
    }

    /**
     * Добавляет деньги в казну гильдии
     */ addToGuildBalance(UUID guildId, double amount) {
        if (amount <= 0) return false;

        double currentBalance = getGuildBalance(guildId);
        setGuildBalance(guildId, currentBalance + amount);
        return true;
    }

    /**
     * Снимает деньги с казны гильдии
     */
    public boolean removeFromGuildBalance(UUID guildId, double amount) {
        if (amount <= 0) return false;

        double currentBalance = getGuildBalance(guildId);
        if (currentBalance < amount) return false;

        setGuildBalance(guildId, currentBalance - amount);
        return true;
    }

    /**
     * Переводит деньги от игрока в казну гильдии
     */
    public boolean depositToGuild(Player player, UUID guildId, double amount) {
        if (economy == null || amount <= 0) return false; (!economy.has(player, amount)) {
            return false;
        }

        if (economy.withdrawPlayer(player, amount).transactionSuccess()) {
            addToGuildBalance(guildId, amount);
            return true;
        }

        return false;
    }

    /**
     * Переводит деньги из казны гильдии игроку
     */
    public boolean withdrawFromGuild(Player player, UUID guildId, double amount) {
        if (economy == null || amount <= 0) return false;

        if (!removeFromGuildBalance(guildId, amount)) {
            return false;
        }

        if (economy.depositPlayer(player, amount).transactionSuccess()) {
            return true;
        } else {
            // Возвращаем деньги обратно в казну если перевод не удался
            addToGuildBalance(guildId, amount);
            return false;
        }
    }

    /**
     * Переводит деньги между гильдиями
     */
    public boolean transferBetweenGuilds(UUID fromGuildId, UUID toGuildId, double amount) {
        if (amount <= 0) return false;

        if (removeFromGuildBalance(fromGuildId, amount)) {
            addToGuildBalance(toGuildId, amount);
            return true;
        }

        return false;
    }

    /**
     * Сохраняет баланс гильдии в базу данных
     */
    private void saveGuildBalance(UUID guildId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "INSERT INTO guild_economy (guild_id, balance) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE balance = VALUES(balance)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, guildId.toString());
                stmt.setDouble(2, getGuildBalance(guildId));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка сохранения баланса гильдии: " + e.getMessage());
        }
    }

    /**
     * Форматирует сумму для отображения
     */
    public String formatMoney(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("%.2f", amount);
    }

    /**
     * Проверяет, подключена ли экономическая система
     */
    public boolean isEconomyEnabled() {
        return economy != null;
    }

    /**
     * Удаляет экономические данные гильдии
     */
    public void deleteGuildEconomy(UUID guildId) {
        guildBalances.remove(guildId);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "DELETE FROM guild_economy WHERE guild_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, guildId.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка удаления экономических данных гильдии: " + e.getMessage());
        }
    }
}
