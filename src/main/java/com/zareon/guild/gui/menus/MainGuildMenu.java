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

public class MainGuildMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public MainGuildMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("main-menu")
                .replace("{guild}", guild.getName());

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Members button
        ItemStack membersItem = createMenuItem("members", guild);
        inventory.setItem(10, membersItem);

        // Level up button
        ItemStack levelUpItem = createMenuItem("level-up", guild);
        inventory.setItem(12, levelUpItem);

        // Regions button
        ItemStack regionsItem = createMenuItem("regions", guild);
        inventory.setItem(14, regionsItem);

        // Flags button
        ItemStack flagsItem = createMenuItem("flags", guild);
        inventory.setItem(16, flagsItem);

        // TNT button
        ItemStack tntItem = createMenuItem("tnt", guild);
        inventory.setItem(28, tntItem);

        // Guild info
        ItemStack guildInfoItem = createGuildInfoItem(guild);
        inventory.setItem(4, guildInfoItem);

        // Fill empty slots with glass pane
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.displayName(miniMessage.deserialize("<gray> "));
        glassPane.setItemMeta(glassMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glassPane);
            }
        }

        return inventory;
    }

    private ItemStack createMenuItem(String menuKey, Guild guild) {
        Material material = Material.valueOf(plugin.getConfigManager().getGUIItemMaterial("main-menu." + menuKey));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfigManager().getGUIItemName("main-menu." + menuKey);
        meta.displayName(miniMessage.deserialize(name));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("main-menu." + menuKey);
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{guild}", guild.getName())
                        .replace("{level}", String.valueOf(guild.getLevel()))
                        .replace("{members}", String.valueOf(guild.getMembers().size()))))
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuildInfoItem(Guild guild) {
        Material material = Material.valueOf(plugin.getConfigManager().getGUIItemMaterial("main-menu.guild-info"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfigManager().getGUIItemName("main-menu.guild-info")
                .replace("{guild}", guild.getName());
        meta.displayName(miniMessage.deserialize(name));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("main-menu.guild-info");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{guild}", guild.getName())
                        .replace("{level}", String.valueOf(guild.getLevel()))
                        .guild.getMembers().size()))}", Bukkit.getOfflinePlayer(guild.getLeader()).getName())
            .replace("{created}", guild.getCreatedDate().toString())))
            .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
}

@Override
public void handleClick(InventoryClickEvent event) {
    Player player = (Player) event.getWhoClicked();
    int slot = event.getSlot();

    switch (slot) {
        case 10 -> plugin.getGuiManager().openMembersMenu(player);
        case 12 -> plugin.getGuiManager().openLevelUpMenu(player);
        case 14 -> plugin.getGuiManager().openRegionMenu(player);
        case 16 -> {
            Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).orElse(null);
            if (guild != null) {
                GuildMember member = guild.getMember(player.getUniqueId());
                if (member.getRank().hasPermission("view_flags")) {
                    plugin.getGuiManager().openFlagsMenu(player);
                } else {
                    player.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getMessage("gui.no-permission")));
                }
            }
        }
        case 28 -> plugin.getGuiManager().openTNTMenu(player);
    }
}

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Nothing special needed on close
    }
}
