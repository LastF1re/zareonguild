package com.zareon.guild.hook;

import com.zareon.guild.ZareonGuildPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class EconomyManager {
    private final ZareonGuildPlugin plugin;
    private Economy econ;
    private boolean enabled;

    public EconomyManager(ZareonGuildPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) {
            enabled = false;
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found; economy disabled.");
            enabled = false;
            return;
        }
        var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault economy provider not found; economy disabled.");
            enabled = false;
            return;
        }
        econ = rsp.getProvider();
        enabled = econ != null;
    }

    public boolean isEnabled() { return enabled; }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return true;
        if (amount <= 0) return true;
        if (econ.getBalance(player) < amount) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!enabled) return;
        econ.depositPlayer(player, amount);
    }
}
