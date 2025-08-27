package com.zareon.guild.gui.menus;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.gui.GUIMenu;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildMember;
import com.zareon.guild.models.GuildRank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MembersMenu implements GUIMenu {
    private final ZareonGuild plugin;
    private final MiniMessage miniMessage;

    public MembersMenu(ZareonGuild plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Inventory createInventory(Player player, Guild guild) {
        String title = plugin.getConfigManager().getGUITitle("members-menu")
                .replace("{guild}", guild.getName());

        Inventory inventory = Bukkit.createInventory(null, 54,
                miniMessage.deserialize(title));

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(49, backItem);

        // Add member items
        List<UUID> members = new ArrayList<>(guild.getMembers());
        GuildMember playerMember = guild.getMember(player.getUniqueId());

        for (int i = 0; i < Math.min(members.size(), 36); i++) {
            UUID memberUUID = members.get(i);
            GuildMember member = guild.getMember(memberUUID);

            ItemStack memberItem = createMemberItem(memberUUID, member, playerMember, guild);
            inventory.setItem(i + 9, memberItem); // Start from slot 9

            // Add promote/demote buttons if player has permission
            if (canManageRank(playerMember, member, guild)) {
                // Promote button
                if (canPromote(member)) {
                    ItemStack promoteItem = createPromoteButton(memberUUID);
                    inventory.setItem(i + 45, promoteItem); // Bottom row
                }

                // Demote button
                if (canDemote(member)) {
                    ItemStack demoteItem = createDemoteButton(memberUUID);
                    inventory.setItem(i + 45, demoteItem); // Bottom row
                }
            }
        }

        return inventory;
    }

    private ItemStack createMemberItem(UUID memberUUID, GuildMember member, GuildMember viewer, Guild guild) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        String player Bukkit.getOfflinePlayer(memberUUID).getName();
        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("members-menu.member")
                        .replace("{player}", playerName)
                        .replace("{rank}", member.getRank().getName())
        ));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("members-menu.member");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(line -> miniMessage.deserialize(line
                        .replace("{player}", playerName)
                        .replace("{rank}", member.getRank().getName())
                        .replace("{joined}", member.getJoinedDate().toString())
                        .replace("{online}", Bukkit.getPlayer(memberUUID) != null ? "Online" : "Offline")))
                .toList();

        meta.lore(lore);
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUUID));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createPromoteButton(UUID memberUUID) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("members-menu.promote")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("members-menu.promote");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDemoteButton(UUID memberUUID) {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("members-menu.demote")));

        List<String> loreList = plugin.getConfigManager().getGUIItemLore("members-menu.demote");
        List<net.kyori.adventure.text.Component> lore = loreList.stream()
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(
                plugin.getConfigManager().getGUIItemName("common.back")));

        item.setItemMeta(meta);
        return item;
    }

    private boolean canManageRank(GuildMember manager, GuildMember target, Guild guild) {
        if (guild.getLeader().equals(manager.getPlayerUUID())) return true;
        return manager.getRank().hasPermission("manage_ranks") &&
                manager.getRank().getPriority() > target.getRank().getPriority();
    }

    private boolean canPromote(GuildMember member) {
        GuildRank nextRank = plugin.getRankManager().getNextRank(member.getRank());
        return nextRank != null && !nextRank.getName().equals("LEADER");
    }

    private boolean canDemote(GuildMember member) {
        GuildRank prevRank = plugin.getRankManager().getPreviousRank(member.getRank());
        return prevRank != null;
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

        // Handle member management
        if (slot >= 9 && slot < 45) {
            int memberIndex = slot - 9;
            List<UUID> members = new ArrayList<>(guild.getMembers());

            if (memberIndex < members.size()) {
                UUID targetUUID = members.get(memberIndex);
                handleMemberAction(player, targetUUID, guild, slot);
            }
        }

        // Handle promote/demote buttons in bottom row
        if (slot >= 45 && slot < 54) {
            int memberIndex = slot - 45;
            List<UUID> members = new ArrayList<>(guild.getMembers());

            if (memberIndex < members.size()) {
                UUID targetUUID = members.get(memberIndex);
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem != null) {
                    if (clickedItem.getType() == Material.GREEN_WOOL) {
                        handlePromote(player, targetUUID, guild);
                    } else if (clickedItem.getType() == Material.RED_WOOL) {
                        handleDemote(player, targetUUID, guild);
                    }
                }
            }
        }
    }

    private void handleMemberAction(Player player, UUID targetUUID, Guild guild, int slot) {
        // Could add member info or other actions here
        // For now, just show member info in chat
        GuildMember member = guild.getMember(targetUUID);
        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("members.info")
                        .replace("{player}", targetName)
                        .replace("{rank}", member.getRank().getName())
                        .replace("{joined}", member.getJoinedDate().toString())
        ));
    }

    private void handlePromote(Player player, UUID targetUUID, Guild guild) {
        GuildMember playerMember = guild.getMember(player.getUniqueId());
        GuildMember targetMember = guild.getMember(targetUUID);

        if (!canManageRank(playerMember, targetMember, guild)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("members.no-permission")));
            return;
        }

        if (!canPromote(targetMember)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("members.cannot-promote")));
            return;
        }

        GuildRank nextRank = plugin.getRankManager().getNextRank(targetMember.getRank());
        targetMember.setRank(nextRank);

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("members.promoted")
                        .replace("{player}", targetName)
                        .replace("{rank}", nextRank.getName())
        ));

        // Refresh menu
        plugin.getGuiManager().refreshMenu(player);
    }

    private void handleDemote(Player player, UUID targetUUID, Guild guild) {
        GuildMember playerMember = guild.getMember(player.getUniqueId());
        GuildMember targetMember = guild.getMember(targetUUID);

        if (!canManageRank(playerMember, targetMember, guild)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("members.no-permission")));
            return;
        }

        if (!canDemote(targetMember)) {
            player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getMessage("members.cannot-demote")));
            return;
        }

        GuildRank prevRank = plugin.getRankManager().getPreviousRank(targetMember.getRank());
        targetMember.setRank(prevRank);

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getMessage("members.demoted")
                        .replace("{player}", targetName)
                        .replace("{rank}", prevRank.getName())
        ));

        // Refresh menu
        plugin.getGuiManager().refreshMenu(player);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Nothing special needed on close
    }
}
