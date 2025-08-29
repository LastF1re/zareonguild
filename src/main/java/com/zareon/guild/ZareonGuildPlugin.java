package com.zareon.guild;

import com.zareon.guild.command.GuildCommand;
import com.zareon.guild.hook.EconomyManager;
import com.zareon.guild.hook.ItemsAdderHook;
import com.zareon.guild.hook.PapiExpansion;
import com.zareon.guild.hook.WorldGuardHook;
import com.zareon.guild.manager.*;
import com.zareon.guild.listener.CraftListeners;
import com.zareon.guild.listener.ExplodeListeners;
import com.zareon.guild.listener.GuildListeners;
import com.zareon.guild.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ZareonGuildPlugin extends JavaPlugin {

    private static ZareonGuildPlugin instance;

    private MessageService messages;
    private EconomyManager economy;
    private GuildManager guildManager;
    private RankManager rankManager;
    private GUIManager guiManager;
    private RegionManager regionManager;
    private TNTManager tntManager;
    private ItemsAdderHook itemsAdderHook;
    private WorldGuardHook worldGuardHook;

    public static ZareonGuildPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.messages = new MessageService(this);
        this.economy = new EconomyManager(this);
        this.rankManager = new RankManager(this);
        this.guildManager = new GuildManager(this, rankManager);
        this.itemsAdderHook = new ItemsAdderHook(this);
        this.worldGuardHook = new WorldGuardHook(this);
        this.regionManager = new RegionManager(this, worldGuardHook);
        this.tntManager = new TNTManager(this);
        this.guiManager = new GUIManager(this, messages, guildManager, rankManager, economy, regionManager, tntManager);

        registerCommands();
        registerListeners();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiExpansion(this, guildManager, rankManager).register();
        }

        getLogger().info("ZareonGuild enabled.");
    }

    @Override
    public void onDisable() {
        guildManager.saveAll();
        getLogger().info("ZareonGuild disabled.");
    }

    private void registerCommands() {
        GuildCommand guildCommand = new GuildCommand(this, messages, guildManager, rankManager, economy, guiManager);
        getCommand("guild").setExecutor(guildCommand);
        getCommand("guild").setTabCompleter(guildCommand);
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GuildListeners(this, messages, guildManager), this);
        pm.registerEvents(new CraftListeners(this, messages, guildManager, tntManager), this);
        pm.registerEvents(new ExplodeListeners(this, guildManager, tntManager, regionManager), this);
    }

    public MessageService messages() { return messages; }
    public EconomyManager economy() { return economy; }
    public GuildManager guilds() { return guildManager; }
    public RankManager ranks() { return rankManager; }
    public GUIManager gui() { return guiManager; }
    public RegionManager regions() { return regionManager; }
    public TNTManager tnt() { return tntManager; }
    public ItemsAdderHook itemsAdder() { return itemsAdderHook; }
    public WorldGuardHook worldGuard() { return worldGuardHook; }
}
