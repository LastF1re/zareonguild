package com.zareon.guild.manager;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import com.zareon.guild.util.ItemUtil;
import com.zareon.guild.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import java.util.List;
import java.util.Map;

public class TNTManager {
    private final ZareonGuildPlugin plugin;
    private final NamespacedKey smallKey;
    private final NamespacedKey mediumKey;
    private final NamespacedKey largeKey;

    public TNTManager(ZareonGuildPlugin plugin) {
        this.plugin = plugin;
        this.smallKey = new NamespacedKey(plugin, "guild_tnt_small");
        this.mediumKey = new NamespacedKey(plugin, "guild_tnt_medium");
        this.largeKey = new NamespacedKey(plugin, "guild_tnt_large");
        registerRecipes();
    }

    private void registerRecipes() {
        register("tnt.small", smallKey);
        register("tnt.medium", mediumKey);
        register("tnt.large", largeKey);
    }

    private void register(String path, NamespacedKey key) {
        List<String> shape = plugin.getConfig().getStringList(path + ".recipe.shape");
        var ingSec = plugin.getConfig().getConfigurationSection(path + ".recipe.ingredients");
        if (shape == null || ingSec == null) return;

        ItemStack result = ItemUtil.simple(Material.TNT,
                plugin.getConfig().getString(path + ".display", "<yellow>Guild TNT</yellow>"),
                List.of("<gray>Guild-only explosive</gray>"));
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(new String[0]));
        for (String k : ingSec.getKeys(false)) {
            Material m = Material.matchMaterial(ingSec.getString(k));
            if (m != null) recipe.setIngredient(k.charAt(0), m);
        }
        try {
            Bukkit.removeRecipe(key);
        } catch (Throwable ignored) {}
        Bukkit.addRecipe(recipe);
    }

    public boolean isGuildTNT(ItemStack stack) {
        if (stack == null || stack.getType() != Material.TNT) return false;
        Component name = ItemUtil.displayName(stack);
        if (name == null) return false;
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name);
        return plain.toLowerCase().contains("guild tnt");
    }

    public void showRecipes(Player player, Guild g) {
        MessageService msg = plugin.messages();
        player.sendMessage(Component.empty().append(msg.prefix())
                .append(msg.inline("<yellow>Guild TNT recipes (requires levels): small=2, medium=3, large=5</yellow>", Map.of())));
    }

    public double powerFor(ItemStack tnt) {
        // Match by display snippets
        String small = plugin.getConfig().getString("tnt.small.display", "").replaceAll("<.*?>", "");
        String medium = plugin.getConfig().getString("tnt.medium.display", "").replaceAll("<.*?>", "");
        String large = plugin.getConfig().getString("tnt.large.display", "").replaceAll("<.*?>", "");
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(ItemUtil.displayName(tnt));
        if (name.contains(small)) return plugin.getConfig().getDouble("tnt.small.power", 6.0);
        if (name.contains(medium)) return plugin.getConfig().getDouble("tnt.medium.power", 9.0);
        if (name.contains(large)) return plugin.getConfig().getDouble("tnt.large.power", 12.0);
        return 4.0;
    }

    public int requiredLevel(ItemStack tnt) {
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(ItemUtil.displayName(tnt));
        if (name.toLowerCase().contains("small")) return plugin.getConfig().getInt("levels.gating.tnt.small", 2);
        if (name.toLowerCase().contains("medium")) return plugin.getConfig().getInt("levels.gating.tnt.medium", 3);
        if (name.toLowerCase().contains("large")) return plugin.getConfig().getInt("levels.gating.tnt.large", 5);
        return Integer.MAX_VALUE;
    }
}
