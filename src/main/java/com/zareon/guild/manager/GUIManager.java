package com.zareon.guild.manager;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.data.GuildMember;
import com.zareon.guild.data.Rank;
import com.zareon.guild.util.ItemUtil;
import com.zareon.guild.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import com.zareon.guild.hook.EconomyManager;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class GUIManager {

    private final ZareonGuildPlugin plugin;
    private final MessageService msg;
    private final GuildManager guilds;
    private final RankManager ranks;
    private final EconomyManager econ;
    private final RegionManager regions;
    private final TNTManager tnts;

    public GUIManager(ZareonGuildPlugin plugin, MessageService msg, GuildManager guilds, RankManager ranks,
                      EconomyManager econ, RegionManager regions, TNTManager tnts) {
        this.plugin = plugin;
        this.msg = msg;
        this.guilds = guilds;
        this.ranks = ranks;
        this.econ = econ;
        this.regions = regions;
        this.tnts = tnts;
    }

    public void openMain(Player player) {
        Guild g = guilds.getGuildByPlayer(player.getUniqueId());
        if (g == null) return;

        int size = plugin.getConfig().getInt("gui.menu.size", 54);
        String title = plugin.getConfig().getString("gui.menu.title", "<green>Guild Menu</green>");
        Inventory inv = Bukkit.createInventory(new MenuHolder(), size, msg.inline(title, Map.of()));

        // Fillers
        ItemStack filler = ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.filler.material", "GRAY_STAINED_GLASS_PANE")),
                plugin.getConfig().getString("gui.menu.items.filler.name", "<gray>-</gray>"),
                List.of());
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // Members grid
        int start = plugin.getConfig().getInt("gui.menu.layout.members_start_slot", 10);
        int perRow = plugin.getConfig().getInt("gui.menu.layout.members_per_row", 7);

        List<Map.Entry<UUID, GuildMember>> sorted = g.getMembers().entrySet().stream()
                .sorted(Comparator.comparing(e -> ranks.index(e.getValue().getRank())))
                .collect(Collectors.toList());

        int slot = start;
        for (var e : sorted) {
            UUID uuid = e.getKey();
            GuildMember m = e.getValue();
            OfflinePlayer op = guilds.offline(uuid);

            ItemStack head = ItemUtil.playerHead(op, "<white>" + op.getName() + "</white>",
                    List.of("<gray>Rank: </gray><yellow>" + m.getRank().name() + "</yellow>"));
            inv.setItem(slot, head);

            // Promote/Demote buttons
            var pItem = ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.promote.material", "LIME_DYE")),
                    plugin.getConfig().getString("gui.menu.items.promote.name", "<green>Promote</green>"),
                    plugin.getConfig().getStringList("gui.menu.items.promote.lore"));
            var dItem = ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.demote.material", "RED_DYE")),
                    plugin.getConfig().getString("gui.menu.items.demote.name", "<red>Demote</red>"),
                    plugin.getConfig().getStringList("gui.menu.items.demote.lore"));

            inv.setItem(slot + 9, ItemUtil.tag(inv, pItem, "act", "promote:" + uuid));
            inv.setItem(slot + 18, ItemUtil.tag(inv, dItem, "act", "demote:" + uuid));

            if ((slot - start + 1) % perRow == 0) {
                slot += (9 - perRow) + 1;
            } else {
                slot += 1;
            }
        }

        // Other actions
        inv.setItem(size - 9, ItemUtil.tag(inv,
                ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.level_up.material", "EXPERIENCE_BOTTLE")),
                        plugin.getConfig().getString("gui.menu.items.level_up.name", "<gold>Level Up Guild</gold>"),
                        plugin.getConfig().getStringList("gui.menu.items.level_up.lore")), "act", "level_up"));

        inv.setItem(size - 8, ItemUtil.tag(inv,
                ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.regions.material", "AMETHYST_SHARD")),
                        plugin.getConfig().getString("gui.menu.items.regions.name", "<light_purple>Your Regions</light_purple>"),
                        plugin.getConfig().getStringList("gui.menu.items.regions.lore")), "act", "regions"));

        inv.setItem(size - 7, ItemUtil.tag(inv,
                ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.flags.material", "TARGET")),
                        plugin.getConfig().getString("gui.menu.items.flags.name", "<yellow>Flags</yellow>"),
                        plugin.getConfig().getStringList("gui.menu.items.flags.lore")), "act", "flags"));

        inv.setItem(size - 6, ItemUtil.tag(inv,
                ItemUtil.simple(Material.valueOf(plugin.getConfig().getString("gui.menu.items.tnt.material", "TNT")),
                        plugin.getConfig().getString("gui.menu.items.tnt.name", "<red>Guild TNT</red>"),
                        plugin.getConfig().getStringList("gui.menu.items.tnt.lore")), "act", "tnt"));

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Guild g = guilds.getGuildByPlayer(player.getUniqueId());
        if (g == null) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        String tag = ItemUtil.getTag(clicked, "act");
        if (tag == null) return;

        if (tag.startsWith("promote:")) {
            UUID target = UUID.fromString(tag.substring("promote:".length()));
            if (guilds.promote(g, player.getUniqueId(), target)) {
                msg.send(player, "rank.promoted", Map.of("player", Objects.toString(guilds.offline(target).getName(), "player"),
                        "rank", g.getMember(target).map(m -> m.getRank().name()).orElse("?")));
                openMain(player);
            } else {
                msg.send(player, "rank.cannot_promote");
            }
        } else if (tag.startsWith("demote:")) {
            UUID target = UUID.fromString(tag.substring("demote:".length()));
            if (guilds.demote(g, player.getUniqueId(), target)) {
                msg.send(player, "rank.demoted", Map.of("player", Objects.toString(guilds.offline(target).getName(), "player"),
                        "rank", g.getMember(target).map(m -> m.getRank().name()).orElse("?")));
                openMain(player);
            } else {
                msg.send(player, "rank.cannot_demote");
            }
        } else if (tag.equals("level_up")) {
            // Costs from config
            int current = g.getLevel();
            int max = plugin.getConfig().getInt("levels.max", 5);
            if (current >= max) {
                msg.send(player, "levels.max_level");
                return;
            }
            double money = plugin.getConfig().getDouble("economy.level_costs." + (current + 1) + ".money", 0.0);
            List<String> res = plugin.getConfig().getStringList("economy.level_costs." + (current + 1) + ".resources");

            if (!ItemUtil.hasResources(player, res)) {
                msg.send(player, "levels.insufficient");
                return;
            }
            if (!econ.withdraw(player, money)) {
                msg.send(player, "levels.insufficient");
                return;
            }
            ItemUtil.takeResources(player, res);
            guilds.setLevel(g, current + 1);
            msg.send(player, "levels.upgraded", Map.of("level", String.valueOf(current + 1)));
            openMain(player);
        } else if (tag.equals("regions")) {
            // Show a simple info, in this version we re-open main
            player.closeInventory();
            player.performCommand("guild members");
        } else if (tag.equals("flags")) {
            player.closeInventory();
            // View-only flags from config
            player.sendMessage(Component.text("Flags:"));
            plugin.getConfig().getConfigurationSection("flags").getKeys(false).forEach(k ->
                    player.sendMessage(Component.text(" - " + k + ": " + plugin.getConfig().get("flags." + k))));
        } else if (tag.equals("tnt")) {
            player.closeInventory();
            tnts.showRecipes(player, g);
        }
    }

    public static class MenuHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return Bukkit.createInventory(this, 9); }
    }
}
