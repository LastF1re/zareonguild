package com.zareon.guild.gui.menus;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.gui.GUIMenu;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import com.zareon.guild.models.PrivateBlock;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class RegionMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public RegionMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("region-menu")
                .replace("{guild}", guild.getName());

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(49, backItem);

        // Private block levels
        displayPrivateBlockLevels(inventory, guild);

        // Current regions list
        displayCurrentRegions(inventory, guild);

        // Region management buttons
        if (canManageRegions(player, guild)) {
            displayManagementButtons(inventory);
        }

        return inventory;
    }

    private void displayPrivateBlockLevels(Inventory inventory, Guild guild) {
        // Level 1 blocks (small)
        ItemStack level1Item = createPrivateBlockItem(1, guild);
        inventory.setItem(10, level1Item);

        // Level 3 blocks (medium)
        ItemStack level3Item = createPrivateBlockItem(3, guild);
        inventory.setItem(12, level3Item);

        // Level 4 blocks (large)
        ItemStack level4Item = createPrivateBlockItem(4, guild);
        inventory.setItem(14, level4Item);
    }

    private ItemStack createPrivateBlockItem(int blockLevel, Guild guild) {
        boolean available = guild.getLevel() >= blockLevel;
        Material material = available ? Material.GREEN_STAINED_GLASS : Material.RED_STAINED_GLASS;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = available ? "available" : "locked";
        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.private-block." + status)
                        .replace("{level}", String.valueOf(blockLevel))
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.private-block." + status);
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{level}", String.valueOf(blockLevel))
                        .replace("{guild_level}", String.valueOf(guild.getLevel()))
                        .replace("{size}", getBlockSize(blockLevel))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void displayCurrentRegions(Inventory inventory, Guild guild) {
        List<PrivateBlock> regions = plugin.getRegionManager().getGuildRegions(guild.getName());
        int slot = 19; // Starting slot for regions

        for (int i = 0; i < Math.min(regions.size(), 7); i++) {
            PrivateBlock region = regions.get(i);
            ItemStack regionItem = createRegionItem(region, i + 1);
            inventory. i, regionItem);
        }
    }

    private ItemStack createRegionItem(PrivateBlock region, int index) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.region")
                        .replace("{index}", String.valueOf(index))
                        .replace("{level}", String.valueOf(region.getLevel()))
        ));

        Location loc = region.getLocation();
        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.region");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{level}", String.valueOf(region.getLevel()))
                        .replace("{x}", String.valueOf(loc.getBlockX()))
                        .replace("{y}", String.valueOf(loc.getBlockY()))
                        .replace("{z}", String.valueOf(loc.getBlockZ()))
                        .replace("{world}", loc.getWorld().getName())
                        .replace("{size}", getBlockSize(region.getLevel()))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void displayManagementButtons(Inventory inventory) {
        // Place block button
        ItemStack placeButton = createPlaceBlockButton();
        inventory.setItem(37, placeButton);

        // Remove block button
        ItemStack removeButton = createRemoveBlockButton();
        inventory.setItem(39, removeButton);

        // Teleport to region button
        ItemStack teleportButton = createTeleportButton();
        inventory.setItem(41, teleportButton);

        // Region info button
        ItemStack infoButton = createRegionInfoButton();
        inventory.setItem(43, infoButton);
    }

    private ItemStack createPlaceBlockButton() {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.place-block")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.place-block");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRemoveBlockButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.remove-block")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.remove-block");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeleportButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.teleport")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.teleport");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRegionInfoButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("region-menu.info")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("region-menu.info");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String getBlockSize(int level) {
        return switch (level) {
            case 1 -> "5x5x5 (Small)";
            case 3 -> "7x7x7 (Medium)";
            case 4 -> "9x9x9 (Large)";
            default -> "Unknown";
        };
    }

    private boolean canManageRegions(Player player, Guild guild) {
        GuildMember member = guild.getMember(player.getUniqueId());
        return member.getRank().hasPermission("manage_regions");
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("common.back")));

        item.setItemMeta(meta);
        return item;
    } event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        switch (slot) {
            case 49 -> plugin.getGuiManager().openMainMenu(player);
            case 10 -> handlePrivateBlockSelect(player, guild, 1);
            case 12 -> handlePrivateBlockSelect(player, guild, 3);
            case 14 -> handlePrivateBlockSelect(player, guild, 4);
            case 37 -> handlePlaceBlock(player, guild);
            case 39 -> handleRemoveBlock(player, guild);
            case 41 -> handleTeleport(player, guild);
            case 43 -> handleRegionInfo(player, guild);
        }

        // Handle region clicks
        if (slot >= 19 && slot <= 25) {
            int regionIndex = slot - 19;
            handleRegionClick(player, guild, regionIndex);
        }
    }

    private void handlePrivateBlockSelect(Player player, Guild guild, int blockLevel) {
        if (guild.getLevel() < blockLevel) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.level-required")
                            .replace("{level}", String.valueOf(blockLevel))));
            return;
        }

        // Store selected block level for placement
        plugin.getRegionManager().setSelectedBlockLevel(player.getUniqueId(), blockLevel);
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("region.block-selected")
                        .replace("{level}", String.valueOf(blockLevel))));

        player.closeInventory();
    }

    private void handlePlaceBlock(Player player, Guild guild) {
        if (!canManageRegions(player, guild)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.no-permission")));
            return;
        }

        Integer selectedLevel = plugin.getRegionManager().getSelectedBlockLevel(player.getUniqueId());
        if (selectedLevel == null) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.no-block-selected")));
            return;
        }

        // Check if player has required materials
        Map<String, Integer> requirements = plugin.getConfigManager().getPrivateBlockRequirements(selectedLevel);
        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            Material material = Material.valueOf(req.getKey().toUpperCase());
            if (!player.getInventory().containsAtLeast(new ItemStack(material), req.getValue())) {
                player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getMessage("region.insufficient-materials")
                                .replace("{material}", req.getKey())
                                .replace("{amount}", String.valueOf(req.getValue()))));
                return;
            }
        }

        // Remove materials
        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            Material material = Material.valueOf(req.getKey().toUpperCase());
            removeItems(player, material, req.getValue());
        }

        // Give player the private block item
        ItemStack blockItem = plugin.getRegionManager().createPrivateBlockItem(selectedLevel);
        player.getInventory().addItem(blockItem);

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("region.block-given")
                        .replace("{level}", String.valueOf(selectedLevel))));

        plugin.getRegionManager().clearSelectedBlockLevel(player.getUniqueId());
        player.closeInventory();
    }

    private void handleRemoveBlock(Player player, Guild guild) {
        if (!canManageRegions(player, guild)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.no-permission")));
            return;
        }

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("region.remove-instructions")));
        player.closeInventory();
    }

    private void handleTeleport(Player player, Guild guild) {
        List<PrivateBlock> regions = plugin.getRegionManager().getGuildRegions(guild.getName());
        if (regions.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.no-regions")));
            return;
        }

        // Teleport to first region for now, could add selection menu
        Location teleportLoc = regions.get(0).getLocation().clone().add(0, 1, 0);
        player.teleport(teleportLoc);

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("region.teleported")));
        player.closeInventory();
    }

    private void handleRegionInfo(Player player, Guild guild) {
        List<PrivateBlock> regions = plugin.getRegionManager().getGuildRegions(guild.getName());

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("region.info.header")
                        .replace("{count}", String.valueOf(regions.size()))));

        for (int i = 0; i < regions.size(); i++) {
            PrivateBlock region = regions.get(i);
            Location loc = region.getLocation();
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.info.format")
                            .replace("{index}", String.valueOf(i + 1))
                            .replace("{level}", String.valueOf(region.getLevel()))
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()))
                            .replace("{world}", loc.getWorld().getName())));
        }
    }

    private void handleRegionClick(Player player, Guild guild, int regionIndex) {
        List<PrivateBlock> regions = plugin.getRegionManager().getGuildRegions(guild.getName());
        if (regionIndex < regions.size()) {
            PrivateBlock region = regions.get(regionIndex);
            Location teleportLoc = region.getLocation().clone().add(0, 1, 0);
            player.teleport(teleportLoc);

            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("region.teleported-to-region")
                            .replace("{index}", String.valueOf(regionIndex + 1))));
            player.closeInventory();
        }
    }

    private void removeItems(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = itemitemAmount <= remaining) {
                    remaining -= itemAmount;
                    contents[i] = null;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        player.getInventory().setContents(contents);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Nothing special needed on close
    }
}
