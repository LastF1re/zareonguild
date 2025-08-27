package com.zareon.guild.gui.menus;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.gui.GUIMenu;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class TNTMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public TNTMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("tnt-menu")
                .replace("{guild}", guild.getName());

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(49, backItem);

        // TNT Level items
        displayTNTLevels(inventory, guild);

        // Craft buttons
        displayCraftButtons(inventory, guild);

        // Information
        displayTNTInfo(inventory);

        return inventory;
    }

    private void displayTNTLevels(Inventory inventory, Guild guild) {
        // Small TNT (requires guild level 2)
        ItemStack smallTNT = createTNTLevelItem(1, "small", guild);
        inventory.setItem(11, smallTNT);

        // Medium TNT (requires guild level 3)
        ItemStack mediumTNT = createTNTLevelItem(2, "medium", guild);
        inventory.setItem(13, mediumTNT);

        // Large TNT (requires guild level 5)
        ItemStack largeTNT = createTNTLevelItem(3, "large", guild);
        inventory.setItem(15, largeTNT);
    }

    private ItemStack createTNTLevelItem(int tntLevel, String sizeName, Guild guild) {
        int requiredGuildLevel = switch (tntLevel) {
            case 1 -> 2; // Small TNT
            case 2 -> 3; // Medium TNT
            case 3 -> 5; // Large TNT
            default -> Integer.MAX_VALUE;
        };

        boolean available = guild.getLevel() >= requiredGuildLevel;
        Material material = available ? Material.TNT : Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = available ? "available" : "locked";
        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("tnt-menu.tnt." + status)
                        .replace("{size}", sizeName)
                        .replace("{level}", String.valueOf(tntLevel))
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("tnt-menu.tnt." + status);
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{size}", sizeName)
                        .replace("{level}", String.valueOf(tntLevel))
                        .replace("{required_level}", String.valueOf(requiredGuildLevel))
                        .replace("{guild_level}", String.valueOf(guild.getLevel()))
                        .replace("{power}", getTNTPower(tntLevel))
                        .replace("{radius}", getTNTRadius(tntLevel))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void displayCraftButtons(Inventory inventory, Guild guild) {
        // Small TNT craft button
        if (guild.getLevel() >= 2) {
            ItemStack smallCraftButton = createCraftButton(1, "small", guild);
            inventory.setItem(29, smallCraftButton);
        }

        // Medium TNT craft button
        if (guild.getLevel() >= 3) {
            ItemStack mediumCraftButton = createCraftButton(2, "medium", guild);
            inventory.setItem(31, mediumCraftButton);
        }

        // Large TNT craft button
        if (guild.getLevel() >= 5) {
            ItemStack largeCraftButton = createCraftButton(3, "large", guild);
            inventory.setItem(33, largeCraftButton);
        }
    }

    private ItemStack createCraftButton(int tntLevel, String sizeName, Guild guild) {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("tnt-menu.craft-button")
                        .replace("{size}", sizeName)
        ));

        Map<String, Integer> recipe = plugin.getConfigManager().getTNTRecipe(tntLevel);
        List<String> loreList = plugin.getConfigManager().getGUIItemLore("tnt-menu.craft-button");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> {
                    String processedLine = line.replace("{size}", sizeName);
                    // Replace recipe placeholders
                    for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
                        processedLine = processedLine.replace("{" + ingredient.getKey() + "}",
                                String.valueOf(ingredient.getValue()));
                    }
                    return miniMessage.deserialize(processedLine);
                })
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void displayTNTInfo(Inventory inventory) {
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta meta = infoItem.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("tnt-menu.info")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("tnt-menu.info");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        infoItem.setItemMeta(meta);
        inventory.setItem(4, infoItem);
    }

    private String getTNTPower(int tntLevel) {
        return switch (tntLevel) {
            case 1 -> "6.0"; // Small TNT
            case 2 -> "8.0"; // Medium TNT
            case 3 -> "12.0"; // Large TNT
            default -> "4.0";
        };
    }

    private String getTNTRadius(int tntLevel) {
        return switch (tntLevel) {
            case 1 -> "6"; // Small TNT
            case 2 -> "8"; // Medium TNT
            case 3 -> "12"; // Large TNT
            default -> "4";
        };
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("common.back")));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
        if (guild == null) return;

        if (slot == 49) { // Back button
            plugin.getGuiManager().openMainMenu(player);
            return;
        }

        // Handle craft button clicks
        switch (slot) {
            case 29 -> handleCraft(player, guild, 1, "small");
            case 31 -> handleCraft(player, guild, 2, "medium");
            case 33 -> handleCraft(player, guild, 3, "large");
            case 11, 13, 15 -> handleTNTInfo(player, slot);
        }
    }

    private void handleCraft(Player player, Guild guild, int tntLevel, String sizeName) {
        GuildMember member = guild.getMember(player.getUniqueId());

        // Check permission
        if (!member.getRank().hasPermission("craft_tnt")) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("tnt.no-permission")));
            return;
        }

        int requiredGuildLevel = switch (tntLevel) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            default -> Integer.MAX_VALUE;
        };

        if (guild.getLevel() < requiredGuildLevel) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("tnt.level-required")
                            .replace("{level}", String.valueOf(requiredGuildLevel))));
            return;
        }

        // Check recipe requirements
        Map<String, Integer> recipe = plugin.getConfigManager().getTNTRecipe(tntLevel);
        for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
            Material material = Material.valueOf(ingredient.getKey().toUpperCase());
            if (!player.getInventory().containsAtLeast(new ItemStack(material), ingredient.getValue())) {
                player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getMessage("tnt.insufficient-materials")
                                .replace("{material}", ingredient.getKey())
                                .replace("{amount}", String.valueOf(ingredient.getValue()))));
                return;
            }
        }

        // Remove materials
        for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
            Material material = Material.valueOf(ingredient.getKey().toUpperCase());
            removeItems(player, material, ingredient.getValue());
        }

        // Give TNT
        ItemStack tntItem = plugin.getTntManager().createCustomTNT(tntLevel);
        player.getInventory().addItem(tntItem);

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("tnt.crafted")
                        .replace("{size}", sizeName)));

        player.closeInventory();
    }

    private void handleTNTInfo(Player player, int slot) {
        int tntLevel = switch (slot) {
            case 11 -> 1; // Small TNT
            case 13 -> 2; // Medium TNT
            case 15 -> 3; // Large TNT
            default -> 0;
        };

        if (tntLevel > 0) {
            String sizeName = switch (tntLevel) {
                case 1 -> "small";
                case 2 -> "medium";
                case 3 -> "large";
                default -> "unknown";
            };

            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("tnt.info")
                            .replace("{size}", sizeName)
                            .replace("{power}", getTNTPower(tntLevel))
                            .replace("{radius}", getTNTRadius(tntLevel))));
        }
    }

    private void removeItems(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
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
