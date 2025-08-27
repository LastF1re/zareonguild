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

public class FlagsMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public FlagsMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("flags-menu")
                .replace("{guild}", guild.getName());

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(49, backItem);

        // Display all flags
        displayFlags(inventory, guild, player);

        return inventory;
    }

    private void displayFlags(Inventory inventory, Guild guild, Player player) {
        Map<String, Object> flags = plugin.getConfigManager().getGuildFlags();
        boolean canModify = canModifyFlags(player, guild);

        int slot = 9; // Starting slot

        for (Map.Entry<String, Object> flagEntry : flags.entrySet()) {
            String flagName = flagEntry.getKey();
            boolean currentValue = guild.getFlag(flagName);

            ItemStack flagItem = createFlagItem(flagName, currentValue, canModify);
            inventory.setItem(slot++, flagItem);

            if (slot >= 45) break; // Don't exceed available slots
        }
    }

    private ItemStack createFlagItem(String flagName, boolean currentValue, boolean canModify) {
        Material material = currentValue ? Material.GREEN_WOOL : Material.RED_WOOL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = currentValue ? "enabled" : "disabled";
        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("flags-menu.flag")
                        .replace("{flag}", flagName)
                        .replace("{status}", status)
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("flags-menu.flag");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> {
                    String processedLine = line
                            .replace("{flag}", flagName)
                            .replace("{status}", status)
                            .replace("{description}", getFlagDescription(flagName))
                            .replace("{can_modify}", canModify ? "Yes" : "No");
                    return miniMessage.deserialize(processedLine);
                })
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String getFlagDescription(String flagName) {
        return switch (flagName.toLowerCase()) {
            case "pvp" -> "Allow PvP in guild regions";
            case "mob_spawning" -> "Allow mobs to spawn in guild regions";
            case "fire_spread" -> "Allow fire to spread in guild regions";
            case "explosion_damage" -> "Allow explosions to damage blocks in guild regions";
            case "interact" -> "Allow non-members to interact with blocks in guild regions";
            case "use" -> "Allow non-members to use items in guild regions";
            case "chest_access" -> "Allow non-members to access chests in guild regions";
            case "build" -> "Allow non-members to build in guild regions";
            case "entry" -> "Allow non-members to enter guild regions";
            case "greeting" -> "Show greeting message when entering guild regions";
            case "farewell" -> "Show farewell message when leaving guild regions";
            case "item_pickup" -> "Allow non-members to pick up items in guild regions";
            case "item_drop" -> "Allow non-members to drop items in guild regions";
            case "enderman_grief" -> "Allow endermen to grief blocks in guild regions";
            case "ghast_fireball" -> "Allow ghast fireballs to damage blocks in guild regions";
            case "other_explosion" -> "Allow other explosions to damage blocks in guild regions";
            case "sleep" -> "Allow non-members to sleep in beds in guild regions";
            case "tnt"ions in guild regions";
                case "lighter" -> "Allow non-members to use lighters in guild regions";
                case "pistons" -> "Allow pistons to function in guild regions";
                case "vehicle_place" -> "Allow non-members to place vehicles in guild regions";
                case "vehicle_destroy" -> "Allow non-members to destroy vehicles in guild regions";
                default -> plugin.getConfigManager().getFlagDescription(flagName);
        };
    }

    private boolean canModifyFlags(Player player, Guild guild) {
        GuildMember member = guild.getMember(player.getUniqueId());
        return member.getRank().hasPermission("modify_flags");
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

        // Handle flag clicks
        if (slot >= 9 && slot < 45) {
            handleFlagClick(player, guild, slot);
        }
    }

    private void handleFlagClick(Player player, Guild guild, int slot) {
        if (!canModifyFlags(player, guild)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("flags.no-permission")));
            return;
        }

        Map<String, Object> flags = plugin.getConfigManager().getGuildFlags();
        String[] flagNames = flags.keySet().toArray(new String[0]);

        int flagIndex = slot - 9;
        if (flagIndex < flagNames.length) {
            String flagName = flagNames[flagIndex];
            boolean currentValue = guild.getFlag(flagName);
            boolean newValue = !currentValue;

            // Check if this flag can be modified by players
            if (!plugin.getConfigManager().isFlagPlayerModifiable(flagName)) {
                player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getMessage("flags.admin-only")
                                .replace("{flag}", flagName)));
                return;
            }

            guild.setFlag(flagName, newValue);

            String status = newValue ? "enabled" : "disabled";
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("flags.changed")
                            .replace("{flag}", flagName)
                            .replace("{status}", status)));

            // Refresh menu
            plugin.getGuiManager().refreshMenu(player);

            // Update WorldGuard region if available
            if (plugin.getRegionManager().hasWorldGuardSupport()) {
                plugin.getRegionManager().updateRegionFlags(guild);
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Nothing special needed on close
    }
}
