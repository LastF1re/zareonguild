package com.zareon.guild.config;

import com.zareon.guild.ZareonGuild;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final ZareonGuild plugin;
    private FileConfiguration config;
    private String prefix;

    public ConfigManager(ZareonGuild plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load prefix
        prefix = config.getString("settings.prefix", "<gold>ZareonGuild</gold> » ");
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMessage(String path) {
        return config.getString("messages." + path, "Message not found: " + path);
    }

    public double getCreationCost() {
        return config.getDouble("economy.creation-cost", 5000);
    }

    public double getRenameCost() {
        return config.getDouble("economy.rename-cost", 2000);
    }

    public boolean isSaveOnChange() {
        return config.getBoolean("settings.save-on-change", true);
    }

    public int getSaveInterval() {
        return config.getInt("settings.save-interval-minutes", 10);
    }

    public int getMaxMembers() {
        return config.getInt("settings.max-members", 20);
    }

    public ConfigurationSection getRanksSection() {
        return config.getConfigurationSection("ranks");
    }

    public ConfigurationSection getLevelsSection() {
        return config.getConfigurationSection("levels");
    }

    public ConfigurationSection getPrivateBlocksSection() {
        return config.getConfigurationSection("private-blocks");
    }

    public ConfigurationSection getTNTSection() {
        return config.getConfigurationSection("tnt");
    }

    public ConfigurationSection getRegionFlagsSection() {
        return config.getConfigurationSection("region-flags");
    }

    public ConfigurationSection getGUISection() {
        return config.getConfigurationSection("gui");
    }

    public boolean isWorldGuardEnabled() {
        return config.getBoolean("settings.use-worldguard", true) && plugin.isWorldGuardEnabled();
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
