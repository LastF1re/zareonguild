package com.zareon.guild;

import com.zareon.guild.commands.GuildCommandHandler;
import com.zareon.guild.config.ConfigManager;
import com.zareon.guild.gui.GUIManager;
import com.zareon.guild.listeners.BlockListener;
import com.zareon.guild.listeners.ExplosionListener;
import com.zareon.guild.listeners.PlayerListener;
import com.zareon.guild.managers.*;
import com.zareon.guild.placeholders.GuildPlaceholders;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ZareonGuild extends JavaPlugin {
    private static ZareonGuild instance;
    private ConfigManager configManager;
    private GuildManager guildManager;
    private RankManager rankManager;
    private RegionManager regionManager;
    private TNTManager tntManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private Economy economy;
    private boolean worldGuardEnabled = false;
    private boolean itemsAdderEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Check for dependencies
        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin detected! Disabling ZareonGuild...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for optional dependencies
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard found! Using WorldGuard for region protection.");
        }

        if (getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            itemsAdderEnabled = true;
            getLogger().info("ItemsAdder found! Custom blocks will be available.");
        }

        // Initialize managers
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        economyManager = new EconomyManager(this, economy);
        rankManager = new RankManager(this);
        guildManager = new GuildManager(this);
        regionManager = new RegionManager(this);
        tntManager = new TNTManager(this);
        guiManager = new GUIManager(this);

        // Register commands
        getCommand("guild").setExecutor(new GuildCommandHandler(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);

        // Register placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildPlaceholders(this).register();
            getLogger().info("PlaceholderAPI found! Registered placeholders.");
        }

        // Load data
        guildManager.loadGuilds();

        getLogger().info("ZareonGuild has been enabled!");
    }

    @Override
    public void onDisable() {
        if (guildManager != null) {
            guildManager.saveGuilds();
        }

        getLogger().info("ZareonGuild has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (economyProvider == null) {
            return false;
        }

        economy = economyProvider.getProvider();
        return economy != null;
    }

    public static ZareonGuild getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public TNTManager getTntManager() {
        return tntManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public boolean isItemsAdderEnabled() {
        return itemsAdderEnabled;
    }
}
