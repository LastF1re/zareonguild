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

public class LevelUpMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public LevelUpMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("levelup-menu")
                .replace("{guild}", guild.getName())
                .replace("{level}", String.valueOf(guild.getLevel()));

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(49, backItem);

        // Current level info
        ItemStack currentLevelItem = createCurrentLevelItem(guild);
        inventory.setItem(4, currentLevelItem);

        // Next level info and upgrade button
        if (guild.getLevel() < 5) {
            ItemStack nextLevelItem = createNextLevelItem(guild);
            inventory.setItem(22, nextLevelItem);

            ItemStack upgradeButton = createUpgradeButton(guild);
            inventory.setItem(40, upgradeButton);
        } else {
            ItemStack maxLevelItem = createMaxLevelItem();
            inventory.setItem(22, maxLevelItem);
        }

        // Level progression display
        for (int level = 1; level <= 5; level++) {
            ItemStack levelItem = createLevelProgressItem(level, guild.getLevel());
            inventory.setItem(9 + level, levelItem);
        }

        // Requirements display
        if (guild.getLevel() < 5) {
            displayRequirements(inventory, guild);
        }

        return inventory;
    }

    private ItemStack createCurrentLevelItem(Guild guild) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("levelup-menu.current-level")
                        .replace("{level}", String.valueOf(guild.getLevel()))
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("levelup-menu.current-level");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{level}", String.valueOf(guild.getLevel()))
                        .replace("{benefits}", getLevelBenefits(guild.getLevel()))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextLevelItem(Guild guild) {
        int nextLevel = guild.getLevel() + 1;
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("levelup-menu.next-level")
                        .replace("{level}", String.valueOf(nextLevel))
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("levelup-menu.next-level");
        List<netComponent> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{level}", String.valueOf(nextLevel))
                        .replace("{benefits}", getLevelBenefits(nextLevel))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeButton(Guild guild) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("levelup-menu.upgrade-button")
        ));

        Map<String, Integer> requirements = plugin.getConfigManager().getLevelUpRequirements(guild.getLevel() + 1);
        double moneyCost = plugin.getConfigManager().getLevelUpCost(guild.getLevel() + 1);

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("levelup-menu.upgrade-button");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> {
                    String processedLine = line.replace("{cost}", String.valueOf(moneyCost));
                    for (Map.Entry<String, Integer> req : requirements.entrySet()) {
                        processedLine = processedLine.replace("{" + req.getKey() + "}",
                                String.valueOf(req.getValue()));
                    }
                    return miniMessage.deserialize(processedLine);
                })
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaxLevelItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("levelup-menu.max-level")
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("levelup-menu.max-level");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLevelProgressItem(int level, int currentLevel) {
        Material material;
        if (level <= currentLevel) {
            material = Material.GREEN_STAINED_GLASS;
        } else if (level == currentLevel + 1) {
            material = Material.YELLOW_STAINED_GLASS;
        } else {
            material = Material.RED_STAINED_GLASS;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = level <= currentLevel ? "completed" :
                level == currentLevel + 1 ? "available" : "locked";

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("levelup-menu.level-progress." + status)
                        .replace("{level}", String.valueOf(level))
        ));

        item.setItemMeta(meta);
        return item;
    }

    private void displayRequirements(Inventory inventory, Guild guild) {
        Map<String, Integer> requirements = plugin.getConfigManager().getLevelUpRequirements(guild.getLevel() + 1);
        int slot = 28; // Starting slot for requirements

        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            ItemStack reqItem = createRequirementItem(req.getKey(), req.getValue());
            inventory.setItem(slot++, reqItem);
        }
    }

    private ItemStack createRequirementItem(String materialName, int amount) {
        Material material = Material.valueOf(materialItemStack item = new ItemStack(material, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                pluginItemName("levelup-menu.requirement")
                        .replace("{material}", materialName)
                        .replace("{amount}", String.valueOf(amount))
        ));

        item.setItemMeta(meta);
        return item;
    }

    private String getLevelBenefits(int level) {
        return switch (level) {
            case 1 -> "Private blocks Lv1";
            case 2 -> "Private blocks Lv1, TNT Small";
            case 3 -> "Private blocks Lv1-3, TNT Small-Medium";
            case 4 -> "Private blocks Lv1-4, TNT Small-Medium";
            case 5 -> "All features unlocked, TNT Small-Large";
            default -> "";
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

        if (slot == 40) { // Upgrade button
            handleUpgrade(player, guild);
        }
    }

    private void handleUpgrade(Player player, Guild guild) {
        GuildMember member = guild.getMember(player.getUniqueId());

        // Check permission
        if (!member.getRank().hasPermission("upgrade_guild")) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("levelup.no-permission")));
            return;
        }

        if (guild.getLevel() >= 5) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("levelup.max-level")));
            return;
        }

        int nextLevel = guild.getLevel() + 1;

        // Check money requirement
        double cost = plugin.getConfigManager().getLevelUpCost(nextLevel);
        if (cost > 0 && !plugin.getEconomyManager().withdrawPlayer(player, cost)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("levelup.insufficient-funds")
                            .replace("{cost}", String.valueOf(cost))));
            return;
        }

        // Check item requirements
        Map<String, Integer> requirements = plugin.getConfigManager().getLevelUpRequirements(nextLevel);
        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            Material material = Material.valueOf(req.getKey().toUpperCase());
            if (!hasItems(player, material, req.getValue())) {
                player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getMessage("levelup.insufficient-items")
                                .replace("{material}", req.getKey())
                                .replace("{amount}", String.valueOf(req.getValue()))));
                return;
            }
        }

        // Remove items
        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            Material material = Material.valueOf(req.getKey().toUpperCase());
            removeItems(player, material, req.getValue());
        }

        // Upgrade guild
        guild.setLevel(nextLevel);

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("levelup.success")
                        .replace("{level}", String.valueOf(nextLevel))));

        // Refresh menu
        plugin.getGuiManager().refreshMenu(player);
    }

    private boolean hasItems(Player player, Material material, int amount) {
        return player.getInventory().containsAtLeast(new ItemStack(material), amount);
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
