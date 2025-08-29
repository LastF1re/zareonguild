package com.zareon.guild.hook;

import com.zareon.guild.ZareonGuildPlugin;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook {
    private final boolean present;

    public ItemsAdderHook(ZareonGuildPlugin plugin) {
        present = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        if (present) {
            plugin.getLogger().info("ItemsAdder detected.");
        }
    }

    public boolean isPresent() { return present; }

    public ItemStack getCustomItem(String namespacedId) {
        if (!present) return null;
        CustomStack cs = CustomStack.getInstance(namespacedId);
        return cs != null ? cs.getItemStack() : null;
    }
}
