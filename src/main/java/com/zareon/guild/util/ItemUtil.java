package com.zareon.guild.util;

import com.zareon.guild.ZareonGuildPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemUtil {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static ItemStack simple(Material mat, String name, List<String> loreRaw) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (name != null) meta.displayName(mm.deserialize(name));
        if (loreRaw != null && !loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String s : loreRaw) lore.add(mm.deserialize(s));
            meta.lore(lore);
        }
        is.setItemMeta(meta);
        return is;
    }

    public static ItemStack playerHead(OfflinePlayer player, String name, List<String> loreRaw) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        if (name != null) meta.displayName(mm.deserialize(name));
        if (loreRaw != null && !loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String s : loreRaw) lore.add(mm.deserialize(s));
            meta.lore(lore);
        }
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack tag(Inventory inv, ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new org.bukkit.NamespacedKey(ZareonGuildPlugin.get(), key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String getTag(ItemStack item, String key) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(ZareonGuildPlugin.get(), key), PersistentDataType.STRING);
    }

    public static Component displayName(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.hasDisplayName() ? meta.displayName() : null;
    }

    public static boolean hasResources(org.bukkit.entity.Player player, List<String> res) {
        if (res == null || res.isEmpty()) return true;
        Map<Material, Integer> need = parse(res);
        for (var e : need.entrySet()) {
            if (count(player.getInventory(), e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    public static void takeResources(org.bukkit.entity.Player player, List<String> res) {
        Map<Material, Integer> need = parse(res);
        for (var e : need.entrySet()) {
            remove(player.getInventory(), e.getKey(), e.getValue());
        }
    }

    private static Map<Material, Integer> parse(List<String> res) {
        Map<Material, Integer> map = new HashMap<>();
        for (String s : res) {
            String[] p = s.split(":");
            Material m = Material.matchMaterial(p[0]);
            int c = p.length > 1 ? Integer.parseInt(p[1]) : 1;
            if (m != null) map.put(m, map.getOrDefault(m, 0) + c);
        }
        return map;
    }

    private static int count(Inventory inv, Material m) {
        int total = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == m) total += it.getAmount();
        }
        return total;
    }

    private static void remove(Inventory inv, Material m, int amount) {
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != m) continue;
            int take = Math.min(amount, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) inv.setItem(i, null);
            amount -= take;
        }
    }
}
