package com.zareon.guild.gui;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.gui.menus.*;
import com.zareon.guild.models.Guild;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {
    private final ZareonGuild plugin;
    private final Map<UUID, GUIMenu> openMenus;

    // Menu instances
    private MainGuildMenu mainGuildMenu;
    private MembersMenu membersMenu;
    private LevelUpMenu levelUpMenu;
    private RegionMenu regionMenu;
    private FlagsMenu flagsMenu;
    private TNTMenu tntMenu;

    public GUIManager(ZareonGuild plugin) {
        this.plugin = plugin;
        <>();

        // Initialize menus
        this.mainGuildMenu = new MainGuildMenu(plugin);
        this.membersMenu = new MembersMenu(plugin);
        this.levelUpMenu = new LevelUpMenu(plugin);
        this.regionMenu = new RegionMenu(plugin);
        this.flagsMenu = new FlagsMenu(plugin);
        this.tntMenu = new TNTMenu(plugin);

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open main guild menu
     */
    public void openMainMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = mainGuildMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), mainGuildMenu);
        player.openInventory(inventory);
    }

    /**
     * Open members management menu
     */
    public void openMembersMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = membersMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), membersMenu);
        player.openInventory(inventory);
    }

    /**
     * Open level up menu
     */
    public void openLevelUpMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = levelUpMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), levelUpMenu);
        player.openInventory(inventory);
    }

    /**
     * Open region menu
     */
    public void openRegionMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = regionMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), regionMenu);
        player.openInventory(inventory);
    }

    /**
     * Open flags menu
     */
    public void openFlagsMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = flagsMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), flagsMenu);
        player.openInventory(inventory);
    }

    /**
     * Open TNT menu
     */
    public void openTNTMenu(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory inventory = tntMenu.createInventory(player, guild);
        openMenus.put(player.getUniqueId(), tntMenu);
        player.openInventory(inventory);
    }

    /**
     * Handle inventory click events for all menus
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GUIMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Handle the click
        menu.handleClick(event);
    }

    /**
     * Handle inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        GUIMenu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            menu.onClose(event);
        }
    }

    /**
     * Check if player has a menu open
     */
    public boolean hasMenuOpen(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }

    /**
     * Get the menu that player has open
     */
    public GUIMenu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }

    /**
     * Close menu for player
     */
    public void closeMenu(Player player) {
        openMenus.remove(player.getUniqueId());
        player.closeInventory();
    }

    /**
     * Refresh menu for player
     */
    public void refreshMenu(Player player) {
        GUIMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        Inventory newInventory = menu.createInventory(player, guild);
        player.openInventory(newInventory);
    }

    /**
     * Get main guild menu
     */
    public MainGuildMenu getMainGuildMenu() {
        return mainGuildMenu;
    }

    /**
     * Get members menu
     */
    public MembersMenu getMembersMenu() {
        return membersMenu;
    }

    /**
     * Get level up menu
     */
    public LevelUpMenu getLevelUpMenu() {
        return levelUpMenu;
    }

    /**
     * Get region menu
     */
    public RegionMenu getRegionMenu() {
        return regionMenu;
    }

    /**
     * Get flags menu
     */
    public FlagsMenu getFlagsMenu() {
        return flagsMenu;
    }

    /**
     * Get TNT menu
     */
    public TNTMenu getTNTMenu() {
        return tntMenu;
    }
}
